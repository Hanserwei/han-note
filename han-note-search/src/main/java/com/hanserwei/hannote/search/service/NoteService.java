package com.hanserwei.hannote.search.service;

import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.hannote.search.model.vo.SearchNoteReqVO;
import com.hanserwei.hannote.search.model.vo.SearchNoteRspVO;

public interface NoteService {

    /**
     * 搜索笔记
     *
     * @param searchNoteReqVO 搜索笔记请求
     * @return 搜索笔记响应
     */
    PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);
}
