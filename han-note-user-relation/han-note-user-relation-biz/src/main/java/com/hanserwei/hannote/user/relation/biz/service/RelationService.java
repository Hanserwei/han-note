package com.hanserwei.hannote.user.relation.biz.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.relation.biz.model.vo.FollowUserReqVO;

public interface RelationService {

    /**
     * 关注用户
     *
     * @param followUserReqVO 关注用户请求
     * @return 响应
     */
    Response<?> follow(FollowUserReqVO followUserReqVO);
}
