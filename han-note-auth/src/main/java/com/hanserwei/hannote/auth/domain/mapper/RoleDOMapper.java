package com.hanserwei.hannote.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.auth.domain.dataobject.RoleDO;

import java.util.List;

public interface RoleDOMapper extends BaseMapper<RoleDO> {

    /**
     * 查询所有被启用的角色
     *
     * @return
     */
    List<RoleDO> selectEnabledList();
}