package com.hanserwei.hannote.note.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import com.hanserwei.hannote.note.biz.model.vo.*;

public interface NoteService extends IService<NoteDO> {

    /**
     * 笔记发布
     * @param publishNoteReqVO 笔记发布请求
     * @return 笔记发布结果
     */
    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    /**
     * 笔记详情
     * @param findNoteDetailReqVO 笔记详情请求
     * @return 笔记详情结果
     */
    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    /**
     * 笔记更新
     * @param updateNoteReqVO 笔记更新请求
     * @return 笔记更新结果
     */
    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

    /**
     * 笔记删除
     * @param deleteNoteReqVO 笔记删除请求
     * @return 笔记删除结果
     */
    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    /**
     * 笔记仅对自己可见
     * @param updateNoteVisibleOnlyMeReqVO 笔记仅对自己可见请求
     * @return 笔记仅对自己可见结果
     */
    Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

    /**
     * 笔记置顶 / 取消置顶
     * @param topNoteReqVO 笔记置顶 / 取消置顶请求
     * @return 笔记置顶 / 取消置顶结果
     */
    Response<?> topNote(TopNoteReqVO topNoteReqVO);

    /**
     * 点赞笔记
     *
     * @param likeNoteReqVO 点赞笔记请求
     * @return 点赞笔记结果
     */
    Response<?> likeNote(LikeNoteReqVO likeNoteReqVO);

    /**
     * 取消点赞笔记
     *
     * @param unlikeNoteReqVO 取消点赞笔记请求
     * @return 取消点赞笔记结果
     */
    Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO);

    /**
     * 收藏笔记
     *
     * @param collectNoteReqVO 收藏笔记请求
     * @return 收藏笔记结果
     */
    Response<?> collectNote(CollectNoteReqVO collectNoteReqVO);

    /**
     * 取消收藏笔记
     *
     * @param unCollectNoteReqVO 取消收藏笔记请求
     * @return 取消收藏笔记结果
     */
    Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO);

}