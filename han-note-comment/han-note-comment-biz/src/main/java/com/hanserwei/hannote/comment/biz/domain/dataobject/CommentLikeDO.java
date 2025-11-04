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
 * 评论点赞表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_comment_like")
public class CommentLikeDO {
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
     * 评论ID
     */
    @TableField(value = "comment_id")
    private Long commentId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}