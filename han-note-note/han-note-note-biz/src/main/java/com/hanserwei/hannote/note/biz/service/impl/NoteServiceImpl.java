package com.hanserwei.hannote.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.note.biz.constant.RedisKeyConstants;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteDOMapper;
import com.hanserwei.hannote.note.biz.enums.NoteStatusEnum;
import com.hanserwei.hannote.note.biz.enums.NoteTypeEnum;
import com.hanserwei.hannote.note.biz.enums.NoteVisibleEnum;
import com.hanserwei.hannote.note.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.note.biz.model.vo.FindNoteDetailReqVO;
import com.hanserwei.hannote.note.biz.model.vo.FindNoteDetailRspVO;
import com.hanserwei.hannote.note.biz.model.vo.PublishNoteReqVO;
import com.hanserwei.hannote.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.hanserwei.hannote.note.biz.rpc.KeyValueRpcService;
import com.hanserwei.hannote.note.biz.rpc.UserRpcService;
import com.hanserwei.hannote.note.biz.service.NoteService;
import com.hanserwei.hannote.note.biz.service.TopicDOService;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    /**
     * 笔记详情本地缓存
     */
    @SuppressWarnings("NullableProblems")
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

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
