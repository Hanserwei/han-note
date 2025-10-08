package com.hanserwei.hannote.note.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import com.hanserwei.hannote.note.biz.model.vo.PublishNoteReqVO;

public interface NoteService extends IService<NoteDO> {

    /**
     * 笔记发布
     * @param publishNoteReqVO 笔记发布请求
     * @return 笔记发布结果
     */
    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

}