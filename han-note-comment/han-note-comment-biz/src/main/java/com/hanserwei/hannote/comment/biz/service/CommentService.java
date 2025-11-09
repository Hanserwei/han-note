package com.hanserwei.hannote.comment.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import com.hanserwei.hannote.comment.biz.model.vo.*;

public interface CommentService extends IService<CommentDO> {
    /**
     * 发布评论
     *
     * @param publishCommentReqVO 发布评论请求
     * @return 响应
     */
    Response<?> publishComment(PublishCommentReqVO publishCommentReqVO);

    /**
     * 评论列表分页查询
     *
     * @param findCommentPageListReqVO 评论列表分页查询参数
     * @return 响应
     */
    PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO findCommentPageListReqVO);

    /**
     * 二级评论分页查询
     *
     * @param findChildCommentPageListReqVO 二级评论分页查询参数
     * @return 响应
     */
    PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(FindChildCommentPageListReqVO findChildCommentPageListReqVO);

    /**
     * 评论点赞
     *
     * @param likeCommentReqVO 评论点赞请求
     * @return 响应
     */
    Response<?> likeComment(LikeCommentReqVO likeCommentReqVO);

    /**
     * 取消评论点赞
     *
     * @param unLikeCommentReqVO 取消评论点赞请求
     * @return 响应
     */
    Response<?> unlikeComment(UnLikeCommentReqVO unLikeCommentReqVO);

    /**
     * 删除评论
     *
     * @param deleteCommentReqVO 删除评论请求
     * @return 响应
     */
    Response<?> deleteComment(DeleteCommentReqVO deleteCommentReqVO);
}
