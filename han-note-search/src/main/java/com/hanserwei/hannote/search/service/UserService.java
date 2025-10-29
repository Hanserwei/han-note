package com.hanserwei.hannote.search.service;

import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.hannote.search.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.model.vo.SearchUserRspVO;

public interface UserService {

    /**
     * 搜索用户
     *
     * @param searchUserReqVO 搜索用户请求
     * @return 搜索用户响应
     */
    PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO);
}
