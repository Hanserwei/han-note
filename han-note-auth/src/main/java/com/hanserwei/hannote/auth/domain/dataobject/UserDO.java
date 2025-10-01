package com.hanserwei.hannote.auth.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_user")
public class UserDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 小哈书号(唯一凭证)
     */
    @TableField(value = "han_note_id")
    private String hanNoteId;

    /**
     * 密码
     */
    @TableField(value = "`password`")
    private String password;

    /**
     * 昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 头像
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 生日
     */
    @TableField(value = "birthday")
    private LocalDate birthday;

    /**
     * 背景图
     */
    @TableField(value = "background_img")
    private String backgroundImg;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 性别(0：女 1：男)
     */
    @TableField(value = "sex")
    private Integer sex;

    /**
     * 状态(0：启用 1：禁用)
     */
    @TableField(value = "`status`")
    private Integer status;

    /**
     * 个人简介
     */
    @TableField(value = "introduction")
    private String introduction;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除(0：未删除 1：已删除)
     */
    @TableField(value = "is_deleted")
    private Boolean isDeleted;
}