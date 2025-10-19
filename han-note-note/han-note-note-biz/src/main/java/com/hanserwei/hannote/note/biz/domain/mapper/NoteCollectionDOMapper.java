package com.hanserwei.hannote.note.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteCollectionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteCollectionDOMapper extends BaseMapper<NoteCollectionDO> {
    /**
     * 新增笔记收藏记录，若已存在，则更新笔记收藏记录
     *
     * @param noteCollectionDO 笔记收藏记录
     * @return 是否成功
     */
    boolean insertOrUpdate(NoteCollectionDO noteCollectionDO);

    /**
     * 取消点赞
     *
     * @param noteCollectionDO 笔记收藏记录
     * @return 影响行数
     */
    int update2UnCollectByUserIdAndNoteId(NoteCollectionDO noteCollectionDO);
}