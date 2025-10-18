package com.hanserwei.hannote.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.DateUtils;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.note.biz.constant.MQConstants;
import com.hanserwei.hannote.note.biz.constant.RedisKeyConstants;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteCollectionDO;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteLikeDO;
import com.hanserwei.hannote.note.biz.domain.dataobject.TopicDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteDOMapper;
import com.hanserwei.hannote.note.biz.enums.*;
import com.hanserwei.hannote.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.hanserwei.hannote.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.hanserwei.hannote.note.biz.model.vo.*;
import com.hanserwei.hannote.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.hanserwei.hannote.note.biz.rpc.KeyValueRpcService;
import com.hanserwei.hannote.note.biz.rpc.UserRpcService;
import com.hanserwei.hannote.note.biz.service.NoteCollectionDOService;
import com.hanserwei.hannote.note.biz.service.NoteLikeDOService;
import com.hanserwei.hannote.note.biz.service.NoteService;
import com.hanserwei.hannote.note.biz.service.TopicDOService;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NoteServiceImpl extends ServiceImpl<NoteDOMapper, NoteDO> implements NoteService {
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private TopicDOService topicDOService;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "noteTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 笔记详情本地缓存
     */
    @SuppressWarnings("NullableProblems")
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();
    @Resource
    private NoteLikeDOService noteLikeDOService;
    @Autowired
    private NoteCollectionDOService noteCollectionDOService;

    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        // 笔记类型
        Integer type = publishNoteReqVO.getType();

        // 获取对应的枚举类型
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 若非图文或视频则抛异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new ApiException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空，默认为空，即true
        boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                //校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空！");
                //校验图片数目
                Preconditions.checkArgument(imgUriList.size() <= 8, "图片不能超过8张！");
                //把图片uri拼接成字符串，逗号隔开
                imgUris = String.join(",", imgUriList);
            }
            case VIDEO -> {
                videoUri = publishNoteReqVO.getVideoUri();
                //校验视频是否为空
                Preconditions.checkArgument(StringUtils.isNoneBlank(videoUri), "笔记视频不能为空！");
            }
            default -> {
            }
        }

        // RPC：调用分布式ID生成服务，生成笔记ID
        String snowflakeId = distributedIdGeneratorRpcService.getSnowflakeId();
        // 笔记内容UUID
        String contentUuid = null;
        // 笔记内容
        String content = publishNoteReqVO.getContent();
        // 若用户填写了笔记内容，则调用KV服务
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
            // 生成笔记内容UUID
            contentUuid = UUID.randomUUID().toString();
            // RPC：调用KV服务，保存笔记内容
            boolean isSaveSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
            // 若保存笔记内容失败，则抛异常
            if (!isSaveSuccess) {
                throw new ApiException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        // 话题
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            //获取话题名称
            topicName = topicDOService.getById(topicId).getName();
        }

        // 发布者ID
        Long creatorId = LoginUserContextHolder.getUserId();

        // 构建笔记对象
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .title(publishNoteReqVO.getTitle())
                .topicId(publishNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();
        try {
            boolean isSaveSuccess = this.save(noteDO);
            if (!isSaveSuccess) {
                throw new ApiException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        } catch (Exception e) {
            log.error("保存笔记失败！", e);
            // RPC：调用KV服务，删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                boolean res = keyValueRpcService.deleteNoteContent(contentUuid);
                if (!res) {
                    log.error("删除笔记内容失败！");
                }
            }
        }
        return Response.success();
    }

    @Override
    @SneakyThrows
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        // 查询笔记ID
        Long noteId = findNoteDetailReqVO.getId();

        // 当前登录用户
        Long userId = LoginUserContextHolder.getUserId();

        // 先从本地缓存中查询
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        if (StringUtils.isNotBlank(findNoteDetailRspVOStrLocalCache)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);
            log.info("==> 笔记详情命中了本地缓存；{}", findNoteDetailRspVOStrLocalCache);
            // 可见性校验
            checkNoteVisibleFromVO(userId, findNoteDetailRspVO);
            return Response.success(findNoteDetailRspVO);
        }

        // 从Redis缓存中获取
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);
        // 若缓存中有该笔记的数据，则直接返回
        if (StringUtils.isNotBlank(noteDetailJson)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);
            // 异步线程中将用户信息存入本地缓存
            threadPoolTaskExecutor.submit(() -> {
                // 写入本地缓存
                LOCAL_CACHE.put(noteId,
                        Objects.isNull(findNoteDetailRspVO) ? "null" : JsonUtils.toJsonString(findNoteDetailRspVO));
            });
            // 可见性校验
            checkNoteVisibleFromVO(userId, findNoteDetailRspVO);

            return Response.success(findNoteDetailRspVO);
        }

        // 查询笔记
        NoteDO noteDO = this.getOne(new LambdaQueryWrapper<>(NoteDO.class)
                .eq(NoteDO::getId, noteId)
                .eq(NoteDO::getStatus, 1));

        // 若笔记不存在，则抛异常
        if (Objects.isNull(noteDO)) {
            threadPoolTaskExecutor.execute(() -> {
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteDetailRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 可见性校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // RPC调用用户服务，获取用户信息
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRspDTO> userResultFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findById(creatorId), threadPoolTaskExecutor);

        // RPC: 调用 K-V 存储服务获取内容
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()), threadPoolTaskExecutor);
        }
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailRspVO> resultFuture = CompletableFuture
                .allOf(userResultFuture, contentResultFuture)
                .thenApply(s -> {
                    // 获取 Future 返回的结果
                    FindUserByIdRspDTO findUserByIdRspDTO = userResultFuture.join();
                    String content = finalContentResultFuture.join();

                    // 笔记类型
                    Integer noteType = noteDO.getType();
                    // 图文笔记图片链接(字符串)
                    String imgUrisStr = noteDO.getImgUris();
                    // 图文笔记图片链接(集合)
                    List<String> imgUris = null;
                    // 如果查询的是图文笔记，需要将图片链接的逗号分隔开，转换成集合
                    if (Objects.equals(noteType, NoteTypeEnum.IMAGE_TEXT.getCode())
                            && StringUtils.isNotBlank(imgUrisStr)) {
                        imgUris = List.of(imgUrisStr.split(","));
                    }

                    // 构建返参 VO 实体类
                    return FindNoteDetailRspVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .topicName(noteDO.getTopicName())
                            .creatorId(noteDO.getCreatorId())
                            .creatorName(findUserByIdRspDTO.getNickName())
                            .avatar(findUserByIdRspDTO.getAvatar())
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();

                });

        // 获取拼装后的 FindNoteDetailRspVO
        FindNoteDetailRspVO findNoteDetailRspVO = resultFuture.get();
        // 异步线程中将笔记详情存入 Redis
        threadPoolTaskExecutor.submit(() -> {
            String noteDetailJson1 = JsonUtils.toJsonString(findNoteDetailRspVO);
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(noteDetailRedisKey, noteDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(findNoteDetailRspVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
        // 笔记ID
        Long noteId = updateNoteReqVO.getId();
        // 笔记类型
        Integer type = updateNoteReqVO.getType();

        // 获取对应枚举类
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 判断笔记类型,如果非图文、视频笔记，则抛出异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new ApiException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }
        String imgUris = null;
        String videoUri = null;

        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");
                imgUris = StringUtils.join(imgUriList, ",");
            }
            case VIDEO -> {
                videoUri = updateNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            default -> {
                // No operation needed, kept for clarity
            }
        }

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = this.getById(noteId);

        // 笔记不存在
        if (Objects.isNull(selectNoteDO)) {
            throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许更新笔记
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new ApiException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 话题
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            TopicDO topicDO = topicDOService.getById(topicId);
            if (Objects.isNull(topicDO)) {
                throw new ApiException(ResponseCodeEnum.TOPIC_NOT_FOUND);
            }
            topicName = topicDO.getName();
            // 判断提交的话题是否真实存在
            if (StringUtils.isBlank(topicName)) {
                throw new ApiException(ResponseCodeEnum.TOPIC_NOT_FOUND);
            }
        }

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 更新笔记元数据表
        String content = updateNoteReqVO.getContent();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(content))
                .imgUris(imgUris)
                .title(updateNoteReqVO.getTitle())
                .topicId(updateNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();
        boolean updateResult = this.updateById(noteDO);
        if (!updateResult) {
            throw new ApiException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        // 一致性保证：延迟双删策略
        // 异步发送延时消息
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId))
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("## 延时删除 Redis 笔记缓存消息发送失败...", e);
                    }
                },
                3000, // 超时时间(毫秒)
                1 // 延迟级别，1 表示延时 1s
        );

        // 删除Redis缓存
        noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 删除本地缓存
        LOCAL_CACHE.invalidate(noteId);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        // 笔记内容更新
        // 查询此篇笔记内容对应的 UUID
        NoteDO noteDO1 = this.getById(noteId);
        String contentUuid = noteDO1.getContentUuid();

        // 笔记内容是否更新成功
        boolean isUpdateContentSuccess = false;
        if (StringUtils.isBlank(content)) {
            // 若笔记内容为空，则删除 K-V 存储
            isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
        } else {
            // 若将无内容的笔记，更新为了有内容的笔记，需要重新生成 UUID
            contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
            // 调用 K-V 更新短文本
            isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
        }

        // 如果更新失败，抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new ApiException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        // 笔记 ID
        Long noteId = deleteNoteReqVO.getId();

        NoteDO selectNoteDO = this.getById(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许删除笔记
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new ApiException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 逻辑删除
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        boolean updateSuccess = this.updateById(noteDO);

        // 若失败，则表示该笔记不存在
        if (!updateSuccess) {
            throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 删除缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记，删除本地缓存发送成功...");

        return Response.success();
    }

    @Override
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        // 笔记 ID
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();

        NoteDO selectNoteDO = this.getById(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许修改笔记权限
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new ApiException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 构建更新的实体类
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .visible(NoteVisibleEnum.PRIVATE.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        // 仅仅更新status为1的数据
        boolean updateResult = this.update(noteDO, new LambdaQueryWrapper<>(NoteDO.class).eq(NoteDO::getStatus, NoteStatusEnum.NORMAL.getCode()));
        if (!updateResult) {
            throw new ApiException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }
        // 删除Redis缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);
        // 同步广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：笔记更新，本地缓存发送成功...");
        return Response.success();
    }

    @Override
    public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
        Long noteId = topNoteReqVO.getId();
        boolean isTop = topNoteReqVO.getIsTop();

        //当前用户ID
        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currUserId) // 只有笔记所有者，才能置顶/取消置顶笔记
                .build();
        boolean isUpdated = this.update(noteDO, new LambdaQueryWrapper<>(NoteDO.class).eq(NoteDO::getId, noteId).eq(NoteDO::getCreatorId, currUserId));
        if (!isUpdated) {
            throw new ApiException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }
        // 删除Redis缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> 笔记置顶更新，本地缓存发送成功...");
        return Response.success();
    }

    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
        Long noteId = likeNoteReqVO.getId();
        // 1. 校验被点赞的笔记是否存在
        checkNoteIsExist(noteId);
        // 2. 判断目标笔记，是否已经点赞过
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器Key
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);

        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);

        // 用户点赞列表 ZSet Key
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        assert noteLikeLuaResultEnum != null;
        switch (noteLikeLuaResultEnum) {
            // Redis 中布隆过滤器不存在
            case NOT_EXIST -> {
                // TODO: 从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器，设置过期时间
                long count = noteLikeDOService.count(new LambdaQueryWrapper<>(NoteLikeDO.class)
                        .eq(NoteLikeDO::getNoteId, noteId)
                        .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode()));

                // 保底1天+随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                // 目标笔记已经被点赞
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() -> batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey));
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                // 若笔记未被点赞，查询当前用户是否点赞其他用户，有则同步初始化布隆过滤器
                batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);

                // 若数据库中也没有点赞记录，说明该用户还未点赞过任何笔记
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被点赞
            case NOTE_LIKED -> {
                // 校验 ZSet 列表中是否包含被点赞的笔记ID
                Double score = redisTemplate.opsForZSet().score(userNoteLikeZSetKey, noteId);

                if (Objects.nonNull(score)) {
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                // 若 Score 为空，则表示 ZSet 点赞列表中不存在，查询数据库校验
                long count = noteLikeDOService.count(new LambdaQueryWrapper<>(NoteLikeDO.class)
                        .eq(NoteLikeDO::getNoteId, noteId)
                        .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode()));
                if (count > 0) {
                    // 数据库里面有点赞记录，而 Redis 中 ZSet 不存在，需要重新异步初始化 ZSet
                    asynInitUserNoteLikesZSet(userId, userNoteLikeZSetKey);
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
            }
        }
        // 3. 更新用户 ZSET 点赞列表
        LocalDateTime now = LocalDateTime.now();
        // lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteLikeLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新点赞的100篇笔记
            Page<NoteLikeDO> page = noteLikeDOService.page(new Page<>(1, 100),
                    new LambdaQueryWrapper<>(NoteLikeDO.class)
                            .select(NoteLikeDO::getNoteId, NoteLikeDO::getCreateTime)
                            .eq(NoteLikeDO::getUserId, userId)
                            .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode())
                            .orderByDesc(NoteLikeDO::getCreateTime));
            List<NoteLikeDO> noteLikeDOS = page.getRecords();
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
            // Lua 脚本路径
            script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
            // 返回值类型
            script2.setResultType(Long.class);

            // 若数据库中存在点赞记录，需要批量同步
            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);

                // 再次调用 note_like_check_and_update_zset.lua 脚本，将点赞的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            } else { // 若数据库中，无点赞过的笔记记录，则直接将当前点赞的笔记 ID 添加到 ZSet 中，随机过期时间
                List<Object> luaArgs = Lists.newArrayList();
                luaArgs.add(DateUtils.localDateTime2Timestamp(LocalDateTime.now())); // score ：点赞时间戳
                luaArgs.add(noteId); // 当前点赞的笔记 ID
                luaArgs.add(expireSeconds); // 随机过期时间

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs.toArray());
            }

        }
        // 4. 发送 MQ, 将点赞数据落库
        // 构建MQ消息体
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeStatusEnum.LIKE.getCode()) // 点赞
                .createTime(now)
                .build();
        // 构建消息，将DTO转换为JSON字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;

        String hashKey = String.valueOf(noteId);
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
        // 笔记ID
        Long noteId = unlikeNoteReqVO.getId();

        // 1. 校验笔记是否真实存在
        checkNoteIsExist(noteId);

        // 2. 校验笔记是否被点赞过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器Key
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_unlike_check.lua")));
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);

        NoteUnlikeLuaResultEnum noteUnlikeLuaResultEnum = NoteUnlikeLuaResultEnum.valueOf(result);
        log.info("==> 【笔记取消点赞】Lua 脚本返回结果: {}", noteUnlikeLuaResultEnum);
        assert noteUnlikeLuaResultEnum != null;
        switch (noteUnlikeLuaResultEnum) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                //笔记不存在
                //异步初始化布隆过滤器
                threadPoolTaskExecutor.submit(() -> {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                    // 从数据库中校验笔记是否被点赞
                    long count = noteLikeDOService.count(new LambdaQueryWrapper<>(NoteLikeDO.class)
                            .eq(NoteLikeDO::getUserId, userId)
                            .eq(NoteLikeDO::getNoteId, noteId)
                            .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode()));
                    if (count == 0) {
                        log.info("==> 【笔记取消点赞】用户未点赞该笔记");
                        throw new ApiException(ResponseCodeEnum.NOTE_NOT_LIKED);
                    }
                });
            }
            // 布隆过滤器校验目标笔记未被点赞（判断绝对正确）
            case NOTE_NOT_LIKED -> throw new ApiException(ResponseCodeEnum.NOTE_NOT_LIKED);
        }

        // 3. 能走到这里，说明布隆过滤器判断已点赞，直接删除 ZSET 中已点赞的笔记 ID
        // 用户点赞列表ZsetKey
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);

        //4. 发送 MQ, 数据更新落库
        // 构建MQ消息体
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode()) // 取消点赞笔记
                .createTime(LocalDateTime.now())
                .build();

        // 构建消息，将DTO转换为JSON字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;

        String hashKey = String.valueOf(noteId);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        // 笔记ID
        Long noteId = collectNoteReqVO.getId();

        // 1. 校验被收藏的笔记是否存在
        checkNoteIsExist(noteId);

        // 2. 判断目标笔记，是否已经收藏过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器Key
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
        // 构建 Redis Key
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);

        NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);
        log.info("==> 【笔记收藏】Lua 脚本返回结果: {}", noteCollectLuaResultEnum);
        assert noteCollectLuaResultEnum != null;
        switch (noteCollectLuaResultEnum) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验笔记是否被收藏，并异步初始化布隆过滤器，设置过期时间
                long count = noteCollectionDOService.count(new LambdaQueryWrapper<>(NoteCollectionDO.class)
                        .eq(NoteCollectionDO::getUserId, userId)
                        .eq(NoteCollectionDO::getNoteId, noteId)
                        .eq(NoteCollectionDO::getStatus, CollectStatusEnum.COLLECT.getCode()));
                // 设置隋朝过期时间
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                // 若目标笔记已经收藏
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() -> {
                        batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                    });
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
                // 若目标笔记未被收藏，查询当前用户是否有收藏其他笔记，有则同步初始化布隆过滤器
                batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                // 添加当前收藏笔记 ID 到布隆过滤器中
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被收藏 (可能存在误判，需要进一步确认)
            case NOTE_COLLECTED -> {
                // 校验ZSet列表中是否有被收藏的笔记ID
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);
                if (Objects.nonNull(score)) {
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
                // 若score为空，则说明该笔记未被收藏，查数据库确认
                long count = noteCollectionDOService.count(new LambdaQueryWrapper<>(NoteCollectionDO.class)
                        .eq(NoteCollectionDO::getUserId, userId)
                        .eq(NoteCollectionDO::getNoteId, noteId)
                        .eq(NoteCollectionDO::getStatus, CollectStatusEnum.COLLECT.getCode()));
                if (count > 0) {
                    // 数据库里面有收藏记录，而 Redis 中 ZSet 已过期被删除的话，需要重新异步初始化 ZSet
                    asynInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);
                    throw new ApiException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
            }
        }

        // 3. 更新用户 ZSET 收藏列表
        // 3. 更新用户 ZSET 收藏列表
        LocalDateTime now = LocalDateTime.now();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新收藏的 300 篇笔记
            Page<NoteCollectionDO> page = noteCollectionDOService.page(new Page<>(1, 300), new LambdaQueryWrapper<>(NoteCollectionDO.class)
                    .select(NoteCollectionDO::getNoteId, NoteCollectionDO::getCreateTime)
                    .eq(NoteCollectionDO::getUserId, userId)
                    .eq(NoteCollectionDO::getStatus, CollectStatusEnum.COLLECT.getCode())
                    .orderByDesc(NoteCollectionDO::getCreateTime));
            List<NoteCollectionDO> noteCollectionDOS = page.getRecords();
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
            // Lua 脚本路径
            script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
            // 返回值类型
            script2.setResultType(Long.class);

            // 若数据库中存在历史收藏笔记，需要批量同步
            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);

                // 再次调用 note_collect_check_and_update_zset.lua 脚本，将当前收藏的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            } else { // 若无历史收藏的笔记，则直接将当前收藏的笔记 ID 添加到 ZSet 中，随机过期时间
                List<Object> luaArgs = Lists.newArrayList();
                luaArgs.add(DateUtils.localDateTime2Timestamp(LocalDateTime.now())); // score：收藏时间戳
                luaArgs.add(noteId); // 当前收藏的笔记 ID
                luaArgs.add(expireSeconds); // 随机过期时间

                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs.toArray());
            }
        }


        // 4. 发送 MQ, 将收藏数据落库
        // 构建消息体 DTO
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectNoteTypeEnum.COLLECT.getCode()) // 收藏笔记
                .createTime(now)
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_COLLECT;

        String hashKey = String.valueOf(userId);

        // 异步发送顺序 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记收藏】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 异步初始化用户收藏笔记 ZSet
     *
     * @param userId                 用户ID
     * @param userNoteCollectZSetKey 用户收藏笔记 ZSet KEY
     */
    private void asynInitUserNoteCollectsZSet(Long userId, String userNoteCollectZSetKey) {
        threadPoolTaskExecutor.submit(() -> {
            // 判断用户笔记收藏 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userNoteCollectZSetKey);

            // 不存在则初始化
            if (!hasKey) {
                // 查询当前用户最新收藏的 300 篇笔记
                Page<NoteCollectionDO> page = noteCollectionDOService.page(new Page<>(1, 300), new LambdaQueryWrapper<>(NoteCollectionDO.class)
                        .select(NoteCollectionDO::getNoteId, NoteCollectionDO::getCreateTime)
                        .eq(NoteCollectionDO::getUserId, userId)
                        .eq(NoteCollectionDO::getStatus, CollectStatusEnum.COLLECT.getCode())
                        .orderByDesc(NoteCollectionDO::getCreateTime));
                List<NoteCollectionDO> noteCollectionDOS = page.getRecords();
                if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 构建笔记收藏 ZSET Lua 脚本参数
     *
     * @param noteCollectionDOS 笔记收藏列表
     * @param expireSeconds     过期时间
     * @return Lua 脚本参数
     */
    private Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int argsLength = noteCollectionDOS.size() * 2 + 1; // 每个笔记收藏关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            // 收藏时间作为 score
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime());
            // 笔记ID 作为 ZSet value
            luaArgs[i + 1] = noteCollectionDO.getNoteId();
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 批量添加用户收藏笔记到布隆过滤器中
     *
     * @param userId                      用户ID
     * @param expireSeconds               过期时间
     * @param bloomUserNoteCollectListKey 布隆过滤器：用户收藏笔记
     */
    private void batchAddNoteCollect2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteCollectListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<NoteLikeDO> noteCollectionDOS = noteLikeDOService.list(new LambdaQueryWrapper<>(NoteLikeDO.class)
                    .select(NoteLikeDO::getNoteId)
                    .eq(NoteLikeDO::getUserId, userId)
                    .eq(NoteLikeDO::getStatus, CollectStatusEnum.COLLECT.getCode()));
            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);

                // 构造Lua参数
                List<Object> luaParams = Lists.newArrayList();
                // 将每个收藏的笔记 ID 传入
                noteCollectionDOS.forEach(noteLikeDO -> luaParams.add(noteLikeDO.getNoteId()));
                // 最后一个参数是过期时间
                luaParams.add(expireSeconds);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), luaParams.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化【笔记收藏】布隆过滤器异常: ", e);
        }
    }

    /**
     * 异步初始化用户点赞笔记 ZSet
     *
     * @param userId              用户ID
     * @param userNoteLikeZSetKey 用户点赞笔记 ZSet KEY
     */
    private void asynInitUserNoteLikesZSet(Long userId, String userNoteLikeZSetKey) {
        threadPoolTaskExecutor.submit(() -> {
            // 判断用户笔记点赞 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userNoteLikeZSetKey);

            // 若不存在，则初始化
            if (!hasKey) {
                // 查询当前用户最新点赞的100篇笔记
                Page<NoteLikeDO> page = noteLikeDOService.page(new Page<>(1, 100),
                        new LambdaQueryWrapper<>(NoteLikeDO.class)
                                .select(NoteLikeDO::getNoteId, NoteLikeDO::getCreateTime)
                                .eq(NoteLikeDO::getUserId, userId)
                                .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode())
                                .orderByDesc(NoteLikeDO::getCreateTime));
                List<NoteLikeDO> noteLikeDOS = page.getRecords();
                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 构建 Lua 脚本参数
     *
     * @param noteLikeDOS   点赞记录
     * @param expireSeconds 过期时间（秒）
     * @return 参数列表
     */
    private Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> noteLikeDOS, long expireSeconds) {
        int argsLength = noteLikeDOS.size() * 2 + 1; // 每个笔记点赞关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteLikeDO noteLikeDO : noteLikeDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime()); // 点赞时间作为 score
            luaArgs[i + 1] = noteLikeDO.getNoteId();          // 笔记ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 异步初始化布隆过滤器
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     * @param bloomUserNoteLikeListKey 布隆过滤器 KEY
     */
    private void batchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<NoteLikeDO> noteLikeDOS = noteLikeDOService.list(new LambdaQueryWrapper<>(NoteLikeDO.class)
                    .select(NoteLikeDO::getNoteId)
                    .eq(NoteLikeDO::getUserId, userId)
                    .eq(NoteLikeDO::getStatus, LikeStatusEnum.LIKE.getCode()));

            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);

                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
                luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化布隆过滤器异常: ", e);
        }
    }

    /**
     * 校验笔记是否存在
     *
     * @param noteId 笔记 ID
     */
    private void checkNoteIsExist(Long noteId) {
        // 先从本地缓存中检验
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        // 解析 JSON 为 FindNoteDetailRspVO
        FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);

        // 若缓存不存在
        if (Objects.isNull(findNoteDetailRspVO)) {
            // 从 Redis 中获取
            String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
            String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

            // 解析字符串为 FindNoteDetailRspVO
            findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);

            // 若 Redis 中不存在，则从数据库中获取

            if (Objects.isNull(findNoteDetailRspVO)) {
                boolean isExist = this.exists(new LambdaQueryWrapper<>(NoteDO.class)
                        .eq(NoteDO::getId, noteId)
                        .eq(NoteDO::getStatus, NoteStatusEnum.NORMAL.getCode()));
                if (!isExist) {
                    throw new ApiException(ResponseCodeEnum.NOTE_NOT_FOUND);
                }
                // 缓存
                threadPoolTaskExecutor.submit(() -> {
                    FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder().id(noteId).build();
                    findNoteDetail(findNoteDetailReqVO);
                });
            }
        }
    }

    /**
     * 校验笔记的可见性
     *
     * @param visible   是否可见
     * @param userId    当前用户 ID
     * @param creatorId 笔记创建者
     */
    private void checkNoteVisible(Integer visible, Long userId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(userId, creatorId)) { // 仅自己可见, 并且访问用户为笔记创建者才能访问，非本人则抛出异常
            throw new ApiException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 校验笔记的可见性（针对 VO 实体类）
     *
     * @param userId              当前用户 ID
     * @param findNoteDetailRspVO 笔记详情VO类
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO findNoteDetailRspVO) {
        if (Objects.nonNull(findNoteDetailRspVO)) {
            Integer visible = findNoteDetailRspVO.getVisible();
            checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
        }
    }
}
