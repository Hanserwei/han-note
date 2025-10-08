package com.hanserwei.hannote.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteDOMapper;
import com.hanserwei.hannote.note.biz.enums.NoteStatusEnum;
import com.hanserwei.hannote.note.biz.enums.NoteTypeEnum;
import com.hanserwei.hannote.note.biz.enums.NoteVisibleEnum;
import com.hanserwei.hannote.note.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.note.biz.model.vo.PublishNoteReqVO;
import com.hanserwei.hannote.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.hanserwei.hannote.note.biz.rpc.KeyValueRpcService;
import com.hanserwei.hannote.note.biz.service.NoteService;
import com.hanserwei.hannote.note.biz.service.TopicDOService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class NoteServiceImpl extends ServiceImpl<NoteDOMapper, NoteDO> implements NoteService {
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private TopicDOService topicDOService;

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
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }
        return Response.success();
    }
}
