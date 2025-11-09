package com.hanserwei.hannote.comment.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.comment.biz.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteCountDOMapper extends BaseMapper<NoteCountDO> {

    /**
     * 查询笔记评论总数
     *
     * @param noteId 笔记ID
     * @return 笔记评论总数
     */
    Long selectCommentTotalByNoteId(Long noteId);

    /**
     * 更新评论总数
     *
     * @param noteId 笔记 ID
     * @param count  评论总数
     * @return 更新数量
     */
    int updateCommentTotalByNoteId(@Param("noteId") Long noteId,
                                   @Param("count") int count);
}