package com.hanserwei.hannote.search.biz.service;

import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.search.biz.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.biz.model.vo.SearchUserRspVO;
import com.hanserwei.hannote.search.dto.RebuildUserDocumentReqDTO;

public interface UserService {

    /**
     * 搜索用户
     *
     * @param searchUserReqVO 搜索用户请求
     * @return 搜索用户响应
     */
    PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO);

    /**
     * 重建用户文档
     *
     * @param rebuildUserDocumentReqDTO 重建用户文档请求
     * @return 响应
     */
    Response<Long> rebuildDocument(RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO);
}
