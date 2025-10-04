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
 * 权限表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_permission")
public class PermissionDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父ID
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 权限名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * 类型(1：目录 2：菜单 3：按钮)
     */
    @TableField(value = "`type`")
    private Byte type;

    /**
     * 菜单路由
     */
    @TableField(value = "menu_url")
    private String menuUrl;

    /**
     * 菜单图标
     */
    @TableField(value = "menu_icon")
    private String menuIcon;

    /**
     * 管理系统中的显示顺序
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 权限标识
     */
    @TableField(value = "permission_key")
    private String permissionKey;

    /**
     * 状态(0：启用；1：禁用)
     */
    @TableField(value = "`status`")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 逻辑删除(0：未删除 1：已删除)
     */
    @TableField(value = "is_deleted")
    private Boolean isDeleted;
}