package com.hanserwei.hannote.kv.biz.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.dto.req.BatchAddCommentContentReqDTO;

public interface CommentContentService {

    Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);
}
