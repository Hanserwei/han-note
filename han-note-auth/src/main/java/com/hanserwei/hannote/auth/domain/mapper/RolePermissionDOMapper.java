package com.hanserwei.hannote.auth.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.auth.domain.dataobject.RolePermissionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RolePermissionDOMapper extends BaseMapper<RolePermissionDO> {
    /**
     * 根据角色 ID 集合批量查询
     *
     * @param roleIds 角色 ID 集合
     * @return 角色权限关系
     */
    List<RolePermissionDO> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}