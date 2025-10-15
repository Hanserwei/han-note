package com.hanserwei.hannote.count.biz.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户计数表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_user_count")
public class UserCountDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 粉丝总数
     */
    @TableField(value = "fans_total")
    private Long fansTotal;

    /**
     * 关注总数
     */
    @TableField(value = "following_total")
    private Long followingTotal;

    /**
     * 发布笔记总数
     */
    @TableField(value = "note_total")
    private Long noteTotal;

    /**
     * 获得点赞总数
     */
    @TableField(value = "like_total")
    private Long likeTotal;

    /**
     * 获得收藏总数
     */
    @TableField(value = "collect_total")
    private Long collectTotal;
}