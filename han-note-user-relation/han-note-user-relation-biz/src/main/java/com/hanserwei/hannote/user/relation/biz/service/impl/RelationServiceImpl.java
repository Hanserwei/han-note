package com.hanserwei.hannote.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.user.dto.req.FindFollowingListReqVO;
import com.hanserwei.hannote.user.dto.resp.FindFollowingUserRspVO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.hannote.user.relation.biz.constant.MQConstants;
import com.hanserwei.hannote.user.relation.biz.constant.RedisKeyConstants;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FollowingDO;
import com.hanserwei.hannote.user.relation.biz.enums.LuaResultEnum;
import com.hanserwei.hannote.user.relation.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hanserwei.hannote.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.hanserwei.hannote.user.relation.biz.model.vo.FollowUserReqVO;
import com.hanserwei.hannote.user.relation.biz.model.vo.UnfollowUserReqVO;
import com.hanserwei.hannote.user.relation.biz.rpc.UserRpcService;
import com.hanserwei.hannote.user.relation.biz.service.FollowingDOService;
import com.hanserwei.hannote.user.relation.biz.service.RelationService;
import com.hanserwei.hannote.user.relation.biz.util.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;
    @Resource
    private FollowingDOService followingDOService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "relationTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 获取被关注用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();

        // 获取当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.equals(userId, followUserId)) {
            throw new ApiException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }
        // 校验关注的用户是否存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);
        if (Objects.isNull(findUserByIdRspDTO)){
            throw new ApiException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 校验当前用户的Zset关注列表是否已经存在
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();

        // Lua脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 转为时间戳
        long timestamp = DateUtils.localDateTime2Timestamp(now);

        // 执行Lua脚本拿到结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // ZSET不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())){
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOService.list(new LambdaQueryWrapper<>(FollowingDO.class)
                    .select(FollowingDO::getUserId)
                    .select(FollowingDO::getFollowingUserId)
                    .select(FollowingDO::getCreateTime)
                    .eq(FollowingDO::getUserId, userId));

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 如果记录为空，直接ZADD关系数据，并设置过期时间
            if (CollUtil.isEmpty(followingDOS)){
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);

                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策略
                redisTemplate.execute(script2, Collections.singletonList(followingRedisKey), followUserId, timestamp, expireSeconds);
            }else {
                // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建Lua参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行Lua脚本,批量同步数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新地关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }

        }

        // 发送 MQ
        // 构造消息体DTO
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                .userId(userId)
                .followUserId(followUserId)
                .createTime(now)
                .build();
        // 构造消息对象，并把DTO转换为JSON字符串设置到消息体中
        Message<String> message = MessageBuilder
                .withPayload(JsonUtils.toJsonString(followUserMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

        log.info("==> 开始发送关注操作 MQ, 消息体: {}", followUserMqDTO);

        // 异步发送MQ消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        // 被取关用户 ID
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();

        // 当前登录用户id
        Long userId = LoginUserContextHolder.getUserId();

        // 无法取关自己
        if (Objects.equals(userId, unfollowUserId)){
            throw new ApiException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }

        // 校验被取关用户是否存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(unfollowUserId);
        if (Objects.isNull(findUserByIdRspDTO)){
            throw new ApiException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 当前用户的关注列表 Redis Key
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);

        // 校验 Lua 脚本执行结果
        // 取关的用户不在关注列表中
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new ApiException(ResponseCodeEnum.NOT_FOLLOWED);
        }

        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) { // ZSET 关注列表不存在
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOService.list(new LambdaQueryWrapper<>(FollowingDO.class).eq(FollowingDO::getUserId, userId));

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followingDOS)) {
                throw new ApiException(ResponseCodeEnum.NOT_FOLLOWED);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：unfollow_check_and_delete.lua , 将取关的用户删除
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);
                // 再次校验结果
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new ApiException(ResponseCodeEnum.NOT_FOLLOWED);
                }
            }
        }


        // 发送MQ
        // 构建消息体DTO
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
                .userId(userId)
                .unfollowUserId(unfollowUserId)
                .createTime(LocalDateTime.now())
                .build();

        // 构造消息对象，并把DTO转换为JSON字符串设置到消息体中
        Message<String> message = MessageBuilder
                .withPayload(JsonUtils.toJsonString(unfollowUserMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;
        log.info("==> 开始发送取关操作 MQ, 消息体: {}", unfollowUserMqDTO);
        // 异步发送MQ消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 取关操作 MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 取关操作 MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
        // 要查询的用户ID
        Long userId = findFollowingListReqVO.getUserId();
        // 页码
        Integer pageNo = findFollowingListReqVO.getPageNo();
        // 先从Redis中查询
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
        // 查询目标用户的关注列表ZSet的总大小
        Long total = redisTemplate.opsForZSet().zCard(followingRedisKey);
        log.info("==> 查询目标用户的关注列表ZSet的总大小{}", total);

        // 构建回参
        List<FindFollowingUserRspVO> findFollowingUserRspVOS = null;

        //每页展示10条数据
        long limit = 10L;
        if (total != null && total > 0) {
            // 缓存有数据
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);

            // 请求页码超过总页数
            if (pageNo > totalPage) {
                log.info("==> 请求页码超过总页数，返回空数据");
                return PageResponse.success(null, pageNo, total);
            }

            // 准备从ZSet中查询分页数据
            // 每页展示10条数据，计算偏移量
            long offset = PageResponse.getOffset(pageNo, limit);

            // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
            // 注意：这里使用了 Double.POSITIVE_INFINITY 和 Double.NEGATIVE_INFINITY 作为分数范围
            // 因为关注列表最多有 1000 个元素，这样可以确保获取到所有的元素
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(followingRedisKey,
                            Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY,
                            offset,
                            limit);
            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                //提取所有ID
                List<Long> userIds = followingUserIdsSet.stream()
                        .map(object -> Long.parseLong(object.toString())).toList();

                log.info("==> 批量查询用户信息，用户ID: {}", userIds);

                // RPC: 批量查询用户信息
                //noinspection ConstantValue
                findFollowingUserRspVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingUserRspVOS);
            }
        } else {
            // 若 Redis 中没有数据，则从数据库查询
            // 先查询记录总量
            long count = followingDOService.count(new LambdaQueryWrapper<>(FollowingDO.class)
                    .eq(FollowingDO::getUserId, userId));
            // 计算一共多少页

            long totalPage = PageResponse.getTotalPage(count, limit);

            // 请求页码超过总页数
            if (pageNo > totalPage) {
                log.info("==> 批量查询用户信息，返回空数据");
                //noinspection DataFlowIssue
                return PageResponse.success(null, pageNo, total);
            }

            // 偏移量
            long offset = PageResponse.getOffset(pageNo, limit);

            // 分页查询
            // 从数据库分页查询
            Page<FollowingDO> page = followingDOService.page(new Page<>(offset / limit + 1, limit),
                    new LambdaQueryWrapper<FollowingDO>()
                            .eq(FollowingDO::getUserId, userId)
                            .orderByDesc(FollowingDO::getCreateTime));
            List<FollowingDO> followingDOS = page.getRecords();
            // 赋值真实地记录总数
            total = count;
            // 若记录不为空
            if (CollUtil.isNotEmpty(followingDOS)) {
                // 提取所有关注用户 ID 到集合中
                List<Long> userIds = followingDOS.stream().map(FollowingDO::getFollowingUserId).toList();

                // RPC: 调用用户服务，并将 DTO 转换为 VO
                //noinspection ConstantValue
                findFollowingUserRspVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingUserRspVOS);

                // 异步将关注列表全量同步到 Redis
                taskExecutor.submit(() -> syncFollowingList2Redis(userId));

            }
        }

        return PageResponse.success(findFollowingUserRspVOS,
                pageNo,
                total);
    }

    /**
     * 全量同步关注列表到 Redis
     *
     * @param userId 用户ID
     */
    private void syncFollowingList2Redis(Long userId) {
        Page<FollowingDO> page = followingDOService.page(new Page<>(1, 1000),
                new LambdaQueryWrapper<>(FollowingDO.class)
                        .select(FollowingDO::getFollowingUserId, FollowingDO::getCreateTime)
                        .eq(FollowingDO::getUserId, userId));
        List<FollowingDO> followingDOS = page.getRecords();
        log.info("==> 全量同步用户关注列表{}", JsonUtils.toJsonString(followingDOS));
        if (CollUtil.isNotEmpty(followingDOS)) {
            // 用户关注列表 Redis Key
            String followingListRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            // 构建 Lua 参数
            Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(followingListRedisKey), luaArgs);
            log.info("==> 全量同步用户关注列表到 Redis，用户ID: {}", userId);
        }

    }

    /**
     * RPC: 调用用户服务，并将 DTO 转换为 VO
     *
     * @param userIds                 用户 ID 列表
     * @param findFollowingUserRspVOS 跟随用户列表
     * @return 跟随用户列表
     */
    private List<FindFollowingUserRspVO> rpcUserServiceAndDTO2VO(List<Long> userIds, List<FindFollowingUserRspVO> findFollowingUserRspVOS) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            findFollowingUserRspVOS = findUserByIdRspDTOS.stream()
                    .map(dto -> FindFollowingUserRspVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build())
                    .toList();
        }
        return findFollowingUserRspVOS;
    }

    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result Lua 脚本返回结果
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new ApiException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new ApiException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    /**
     * 构建 Lua 脚本参数
     *
     * @param followingDOS 关注列表
     * @param expireSeconds 过期时间
     * @return Lua 脚本参数
     */
    private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO following : followingDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(following.getCreateTime()); // 关注时间作为 score
            luaArgs[i + 1] = following.getFollowingUserId();          // 关注的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }
}
