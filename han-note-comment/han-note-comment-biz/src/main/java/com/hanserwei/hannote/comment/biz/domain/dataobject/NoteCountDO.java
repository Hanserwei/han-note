package com.hanserwei.hannote.comment.biz.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记计数表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_note_count")
public class NoteCountDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 笔记ID
     */
    @TableField(value = "note_id")
    private Long noteId;

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

    /**
     * 被评论总数
     */
    @TableField(value = "comment_total")
    private Long commentTotal;
}