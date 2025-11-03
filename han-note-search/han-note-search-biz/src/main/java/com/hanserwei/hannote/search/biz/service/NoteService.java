package com.hanserwei.hannote.search.biz.service;

import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.search.biz.model.vo.SearchNoteReqVO;
import com.hanserwei.hannote.search.biz.model.vo.SearchNoteRspVO;
import com.hanserwei.hannote.search.dto.RebuildNoteDocumentReqDTO;

public interface NoteService {

    /**
     * 搜索笔记
     *
     * @param searchNoteReqVO 搜索笔记请求
     * @return 搜索笔记响应
     */
    PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);

    /**
     * 重建笔记文档
     *
     * @param rebuildNoteDocumentReqDTO 重建笔记文档请求
     * @return 响应
     */
    Response<Long> rebuildDocument(RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO);
}
