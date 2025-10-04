package com.hanserwei.hannote.user.biz.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 角色表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_role")
public class RoleDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色名
     */
    @TableField(value = "role_name")
    private String roleName;

    /**
     * 角色唯一标识
     */
    @TableField(value = "role_key")
    private String roleKey;

    /**
     * 状态(0：启用 1：禁用)
     */
    @TableField(value = "`status`")
    private Byte status;

    /**
     * 管理系统中的显示顺序
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 最后一次更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 逻辑删除(0：未删除 1：已删除)
     */
    @TableField(value = "is_deleted")
    private Boolean isDeleted;
}