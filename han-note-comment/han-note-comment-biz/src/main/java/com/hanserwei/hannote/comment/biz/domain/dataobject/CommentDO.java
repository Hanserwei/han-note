package com.hanserwei.hannote.comment.biz.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_comment")
public class CommentDO {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的笔记ID
     */
    @TableField(value = "note_id")
    private Long noteId;

    /**
     * 发布者用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 评论内容UUID
     */
    @TableField(value = "content_uuid")
    private String contentUuid;

    /**
     * 内容是否为空(0：不为空 1：为空)
     */
    @TableField(value = "is_content_empty")
    private Boolean isContentEmpty;

    /**
     * 评论附加图片URL
     */
    @TableField(value = "image_url")
    private String imageUrl;

    /**
     * 级别(1：一级评论 2：二级评论)
     */
    @TableField(value = "`level`")
    private Integer level;

    /**
     * 评论被回复次数，仅一级评论需要
     */
    @TableField(value = "reply_total")
    private Long replyTotal;

    /**
     * 评论被点赞次数
     */
    @TableField(value = "like_total")
    private Long likeTotal;

    /**
     * 父ID (若是对笔记的评论，则此字段存储笔记ID; 若是二级评论，则此字段存储一级评论的ID)
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 回复哪个的评论 (0表示是对笔记的评论，若是对他人评论的回复，则存储回复评论的ID)
     */
    @TableField(value = "reply_comment_id")
    private Long replyCommentId;

    /**
     * 回复的哪个用户, 存储用户ID
     */
    @TableField(value = "reply_user_id")
    private Long replyUserId;

    /**
     * 是否置顶(0：不置顶 1：置顶)
     */
    @TableField(value = "is_top")
    private Boolean isTop;

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
     * 二级评论总数（只有一级评论才需要统计）
     */
    @TableField(value = "child_comment_total")
    private Long childCommentTotal;

    /**
     * 评论热度
     */
    @TableField(value = "heat")
    private Double heat;

    /**
     * 最早回复的评论ID (只有一级评论需要)
     */
    @TableField(value = "first_reply_comment_id")
    private Long firstReplyCommentId;
}