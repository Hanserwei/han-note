package com.hanserwei.hannote.kv.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentDO;
import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.hanserwei.hannote.kv.biz.domain.repository.CommentContentRepository;
import com.hanserwei.hannote.kv.biz.service.CommentContentService;
import com.hanserwei.hannote.kv.dto.req.*;
import com.hanserwei.hannote.kv.dto.resp.FindCommentContentRspDTO;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentContentServiceImpl implements CommentContentService {

    @Resource
    private CassandraTemplate cassandraTemplate;
    @Resource
    private CommentContentRepository commentContentRepository;

    @Override
    public Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {

        List<CommentContentReqDTO> comments = batchAddCommentContentReqDTO.getComments();

        //DTO转DO

        List<CommentContentDO> contentDOS = comments.stream()
                .map(comment -> {
                    // 构建主键类
                    CommentContentPrimaryKey commentContentPrimaryKey = CommentContentPrimaryKey.builder()
                            .noteId(comment.getNoteId())
                            .yearMonth(comment.getYearMonth())
                            .contentId(UUID.fromString(comment.getContentId()))
                            .build();
                    // 构建DO
                    return CommentContentDO.builder()
                            .primaryKey(commentContentPrimaryKey)
                            .content(comment.getContent())
                            .build();
                }).toList();

        // 批量插入数据
        cassandraTemplate.batchOps()
                .insert(contentDOS)
                .execute();
        return Response.success();
    }

    @Override
    public Response<?> batchFindCommentContent(BatchFindCommentContentReqDTO batchFindCommentContentReqDTO) {
        // 归属的笔记ID
        Long noteId = batchFindCommentContentReqDTO.getNoteId();
        // 查询评论的发布年月、内容 UUID
        List<FindCommentContentReqDTO> commentContentKeys = batchFindCommentContentReqDTO.getCommentContentKeys();

        // 过滤出年月
        List<@NotBlank(message = "发布年月不能为空") String> yearMonths = commentContentKeys.stream()
                .map(FindCommentContentReqDTO::getYearMonth)
                .distinct()
                .toList();
        // 过滤出内容 UUID
        List<UUID> contentIds = commentContentKeys.stream()
                .map(r -> UUID.fromString(r.getContentId()))
                .distinct()
                .toList();
        // 批量查询 Cassandra
        List<CommentContentDO> commentContentDOS = commentContentRepository
                .findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(noteId, yearMonths, contentIds);

        // DO 转 DTO
        List<FindCommentContentRspDTO> findCommentContentRspDTOS = Lists.newArrayList();
        if (CollUtil.isNotEmpty(commentContentDOS)) {
            findCommentContentRspDTOS = commentContentDOS.stream()
                    .map(commentContentDO -> FindCommentContentRspDTO.builder()
                            .contentId(String.valueOf(commentContentDO.getPrimaryKey().getContentId()))
                            .content(commentContentDO.getContent())
                            .build())
                    .toList();
        }

        return Response.success(findCommentContentRspDTOS);
    }

    @Override
    public Response<?> deleteCommentContent(DeleteCommentContentReqDTO deleteCommentContentReqDTO) {
        Long noteId = deleteCommentContentReqDTO.getNoteId();
        String yearMonth = deleteCommentContentReqDTO.getYearMonth();
        String contentId = deleteCommentContentReqDTO.getContentId();

        // 删除评论正文
        commentContentRepository.deleteByPrimaryKeyNoteIdAndPrimaryKeyYearMonthAndPrimaryKeyContentId(noteId, yearMonth, UUID.fromString(contentId));

        return Response.success();
    }
}
