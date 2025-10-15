package com.hanserwei.hannote.count.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteCountDOMapper extends BaseMapper<NoteCountDO> {
}