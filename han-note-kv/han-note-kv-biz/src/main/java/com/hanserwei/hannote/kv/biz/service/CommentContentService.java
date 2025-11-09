package com.hanserwei.hannote.kv.biz.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.DeleteCommentContentReqDTO;

public interface CommentContentService {

    /**
     * 批量添加评论内容
     *
     * @param batchAddCommentContentReqDTO 批量添加评论内容请求参数
     * @return 批量添加结果
     */
    Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);

    /**
     * 批量查询评论内容
     *
     * @param batchFindCommentContentReqDTO 批量查询评论内容请求参数
     * @return 批量查询结果
     */
    Response<?> batchFindCommentContent(BatchFindCommentContentReqDTO batchFindCommentContentReqDTO);

    /**
     * 删除评论内容
     *
     * @param deleteCommentContentReqDTO 删除评论内容请求参数
     * @return 删除结果
     */
    Response<?> deleteCommentContent(DeleteCommentContentReqDTO deleteCommentContentReqDTO);

}
