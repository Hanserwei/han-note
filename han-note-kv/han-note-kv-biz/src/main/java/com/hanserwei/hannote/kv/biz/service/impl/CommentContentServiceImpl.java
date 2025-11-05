package com.hanserwei.hannote.kv.biz.service.impl;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentDO;
import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.hanserwei.hannote.kv.biz.service.CommentContentService;
import com.hanserwei.hannote.kv.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.CommentContentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentContentServiceImpl implements CommentContentService {

    @Resource
    private CassandraTemplate cassandraTemplate;

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
}
