package com.hanserwei.hannote.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.auth.domain.dataobject.PermissionDO;

import java.util.List;

public interface PermissionDOMapper extends BaseMapper<PermissionDO> {
    /**
     * 查询 APP 端所有被启用的权限
     *
     * @return 权限列表
     */
    List<PermissionDO> selectAppEnabledList();
}