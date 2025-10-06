package com.hanserwei.hannote.kv.biz.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.dto.req.AddNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.DeleteNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.FindNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.resp.FindNoteContentRspDTO;

public interface NoteContentService {
    /**
     * 添加笔记内容
     *
     * @param addNoteContentReqDTO 添加笔记内容请求参数
     * @return 响应
     */
    Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    /**
     * 查询笔记内容
     *
     * @param findNoteContentReqDTO 查询笔记内容请求参数
     * @return 响应
     */
    Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    /**
     * 删除笔记内容
     *
     * @param deleteNoteContentReqDTO 删除笔记内容请求参数
     * @return 响应
     */
    Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);

}
