package com.hanserwei.hannote.note.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteDOMapper extends BaseMapper<NoteDO> {
}