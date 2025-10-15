package com.hanserwei.hannote.count.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.UserCountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserCountDOMapper extends BaseMapper<UserCountDO> {
}