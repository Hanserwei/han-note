package com.hanserwei.hannote.user.relation.biz.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.utils.DateUtils;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.user.relation.biz.constant.MQConstants;
import com.hanserwei.hannote.user.relation.biz.constant.RedisKeyConstants;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FansDO;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FollowingDO;
import com.hanserwei.hannote.user.relation.biz.enums.FollowUnfollowTypeEnum;
import com.hanserwei.hannote.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.hannote.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hanserwei.hannote.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.hanserwei.hannote.user.relation.biz.service.FansDOService;
import com.hanserwei.hannote.user.relation.biz.service.FollowingDOService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Component
@RocketMQMessageListener(
        consumerGroup = "han_note_group_" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, //han_note_group_FollowUnfollowTopic
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, //FollowUnfollowTopic
        consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
@RequiredArgsConstructor
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
    private final TransactionTemplate transactionTemplate;
    private final FollowingDOService followingDOService;
    private final FansDOService fansDOService;

    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        // 根据MQ标签判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) {
            // 关注
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) {
            // 取关
            handleUnfollowTagMessage(bodyJsonStr);
        }
    }

    /**
     * 取关
     *
     * @param bodyJsonStr 消息体
     */
    private void handleUnfollowTagMessage(String bodyJsonStr) {
        // 消息体json串转换为DTO对象
        UnfollowUserMqDTO unfollowUserMqDTO = JsonUtils.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(unfollowUserMqDTO)) {
            return;
        }

        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
        LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();

        // 编程式事务提交
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 数据库操作,两个数据库操作
                // 关注表：一条记录
                boolean isRemoved = followingDOService.remove(new LambdaQueryWrapper<>(FollowingDO.class)
                        .eq(FollowingDO::getUserId, userId)
                        .eq(FollowingDO::getFollowingUserId, unfollowUserId));
                if (isRemoved) {
                    // 粉丝表：一条记录
                    return fansDOService.remove(new LambdaQueryWrapper<>(FansDO.class)
                            .eq(FansDO::getUserId, unfollowUserId)
                            .eq(FansDO::getFansUserId, userId));
                }
                return true;
            }catch (Exception e){
                status.setRollbackOnly();
                log.error("## 取关失败, userId: {}, unfollowUserId: {}, createTime: {}", userId, unfollowUserId, createTime);
            }
            return false;
        }));

        // 若数据库删除成功，更新 Redis，将自己从被取关用户的 ZSet 粉丝列表删除
        if (isSuccess) {
            // 被取关用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildUserFansKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(fansRedisKey, userId);

            // 发送MQ消息通知计数服务，统计关注数
            // 构建DTO对象
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(userId)
                    .targetUserId(unfollowUserId)
                    .type(FollowUnfollowTypeEnum.UNFOLLOW.getCode())
                    .build();

            // 发送MQ
            sendMQ(countFollowUnfollowMqDTO);
        }
    }

    /**
     * 关注
     *
     * @param bodyJsonStr 消息体
     */
    private void handleFollowTagMessage(String bodyJsonStr) {
        // 解析消息体转换为DTO对象
        FollowUserMqDTO followUserMqDTO = JsonUtils.parseObject(bodyJsonStr, FollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(followUserMqDTO)) {
            return;
        }

        // 幂等性：通过联合唯一索引保证

        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();

        // 编程式事物
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注成功需往数据库添加两条记录
                // 关注表：一条记录
                boolean followRecordSaved = followingDOService.save(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());
                // 粉丝表：一条记录
                if (followRecordSaved) {
                    return fansDOService.save(FansDO.builder()
                            .userId(followUserId)
                            .fansUserId(userId)
                            .createTime(createTime)
                            .build());
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("## 添加关注关系失败, userId: {}, followUserId: {}, createTime: {}", userId, followUserId, createTime);
            }
            return false;
        }));

        log.info("## 数据库添加记录结果: {}", isSuccess);
        if (isSuccess) {
            // Lua脚本
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);

            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);

            // 构建关注有户的粉丝列表的KEY
            String fansZSetKey = RedisKeyConstants.buildUserFansKey(followUserId);

            // 执行Lua脚本
            redisTemplate.execute(script, Collections.singletonList(fansZSetKey), userId, timestamp);

            // 发送MQ消息通知计数服务，统计关注数
            // 构建消息体
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(userId)
                    .targetUserId(followUserId)
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
                    .build();

            sendMQ(countFollowUnfollowMqDTO);
        }
    }

    /**
     * 发送MQ消息
     *
     * @param countFollowUnfollowMqDTO 消息体
     */
    private void sendMQ(CountFollowUnfollowMqDTO countFollowUnfollowMqDTO) {
        // 构建MQ消息体
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countFollowUnfollowMqDTO))
                .build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数】MQ 发送异常: ", throwable);
            }
        });

        // 发送 MQ 通知计数服务：统计粉丝数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
        });
    }
}
