package com.hanserwei.hannote.count.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteCountDOMapper extends BaseMapper<NoteCountDO> {

    /**
     * 添加笔记计数记录或更新笔记点赞数
     *
     * @param count  计数
     * @param noteId 笔记ID
     * @return 影响行数
     */
    int insertOrUpdateLikeTotalByNoteId(@Param("count") Integer count, @Param("noteId") Long noteId);

    /**
     * 添加记录或更新笔记收藏数
     *
     * @param count  收藏数
     * @param noteId 笔记ID
     * @return 影响行数
     */
    int insertOrUpdateCollectTotalByNoteId(@Param("count") Integer count, @Param("noteId") Long noteId);

    /**
     * 添加记录或更新笔记评论数
     *
     * @param count  评论数
     * @param noteId 笔记ID
     * @return 影响行数
     */
    int insertOrUpdateCommentTotalByNoteId(@Param("count") int count, @Param("noteId") Long noteId);
}