package com.hanserwei.hannote.comment.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import com.hanserwei.hannote.comment.biz.model.vo.PublishCommentReqVO;

public interface CommentService extends IService<CommentDO> {
    /**
     * 发布评论
     *
     * @param publishCommentReqVO 发布评论请求
     * @return 响应
     */
    Response<?> publishComment(PublishCommentReqVO publishCommentReqVO);
}
