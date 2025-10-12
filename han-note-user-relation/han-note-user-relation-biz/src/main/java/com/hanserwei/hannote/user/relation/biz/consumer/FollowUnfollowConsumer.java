package com.hanserwei.hannote.user.relation.biz.consumer;

import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.user.relation.biz.constant.MQConstants;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FansDO;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FollowingDO;
import com.hanserwei.hannote.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hanserwei.hannote.user.relation.biz.service.FansDOService;
import com.hanserwei.hannote.user.relation.biz.service.FollowingDOService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RocketMQMessageListener(
        consumerGroup = "han_note_group_" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW
)
@Slf4j
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
    private final TransactionTemplate transactionTemplate;
    private final FollowingDOService followingDOService;
    private final FansDOService fansDOService;

    public FollowUnfollowConsumer(TransactionTemplate transactionTemplate, FollowingDOService followingDOService, FansDOService fansDOService) {
        this.transactionTemplate = transactionTemplate;
        this.followingDOService = followingDOService;
        this.fansDOService = fansDOService;
    }

    @Override
    public void onMessage(Message message) {
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        // 根据MQ标签判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)){
            // 关注
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) {
            // 取关
            // TODO: 待实现
        }
    }

    /**
     * 关注
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
                if (followRecordSaved){
                    return fansDOService.save(FansDO.builder()
                            .userId(followUserId)
                            .fansUserId(userId)
                            .createTime(createTime)
                            .build());
                }
            }catch (Exception e){
                status.setRollbackOnly();
                log.error("## 添加关注关系失败, userId: {}, followUserId: {}, createTime: {}", userId, followUserId, createTime);
            }
            return false;
        }));

        log.info("## 数据库添加记录结果: {}", isSuccess);
        // TODO: 更新 Redis 中被关注用户的 ZSet 粉丝列表
    }
}
