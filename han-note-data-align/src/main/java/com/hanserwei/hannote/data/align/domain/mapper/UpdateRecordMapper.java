package com.hanserwei.hannote.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

public interface UpdateRecordMapper {

    /**
     * 更新 t_user_count 计数表总关注数
     */
    int updateUserFollowingTotalByUserId(@Param("userId") long userId,
                                         @Param("followingTotal") int followingTotal);

    /**
     * 更新 t_user_count 计数表总粉丝数
     */
    int updateUserFansTotalByUserId(@Param("userId") long userId,
                                    @Param("fansTotal") int fansTotal);

    /**
     * 更新 t_user_count 计数表总笔记数
     */
    int updateUserNotePublishTotalByUserId(@Param("userId") Long userId,
                                           @Param("notePublishTotal") int notePublishTotal);

    /**
     * 更新 t_note_count 计数表笔记点赞数
     */
    int updateNoteLikeTotalByNoteId(@Param("noteId") long noteId,
                                    @Param("noteLikeTotal") int noteLikeTotal);

    /**
     * 更新 t_note_count 计数表笔记收藏数
     */
    int updateNoteCollectTotalByNoteId(@Param("noteId") long noteId,
                                       @Param("noteCollectTotal") int noteCollectTotal);


    /**
     * 更新 t_user_count 计数表总收藏数
     */
    int updateUserCollectTotalByUserId(@Param("userId") Long userId,
                                       @Param("userCollectTotal") int userCollectTotal);

    /**
     * 更新 t_user_count 计数表总点赞数
     */
    int updateUserLikeTotalByUserId(@Param("userId") Long userId,
                                    @Param("userLikeTotal") int userLikeTotal);
}