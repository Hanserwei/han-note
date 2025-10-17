package com.hanserwei.hannote.note.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteLikeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteLikeDOMapper extends BaseMapper<NoteLikeDO> {
    /**
     * 新增笔记点赞记录，若已存在，则更新笔记点赞记录
     *
     * @param noteLikeDO 笔记点赞记录
     * @return 影响行数
     */
    boolean insertOrUpdate(NoteLikeDO noteLikeDO);
}