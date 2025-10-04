package com.hanserwei.hannote.user.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.user.biz.domain.dataobject.RoleDO;

import java.util.List;

public interface RoleDOMapper extends BaseMapper<RoleDO> {

    /**
     * 查询所有被启用的角色
     *
     * @return 角色列表
     */
    List<RoleDO> selectEnabledList();

    RoleDO selectByPrimaryKey(Long commonUserRoleId);
}