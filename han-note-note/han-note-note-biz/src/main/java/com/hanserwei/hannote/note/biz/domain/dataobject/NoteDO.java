package com.hanserwei.hannote.note.biz.domain.dataobject;

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
 * 笔记表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_note")
public class NoteDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 内容是否为空(0：不为空 1：空)
     */
    @TableField(value = "is_content_empty")
    private Boolean isContentEmpty;

    /**
     * 发布者ID
     */
    @TableField(value = "creator_id")
    private Long creatorId;

    /**
     * 话题ID
     */
    @TableField(value = "topic_id")
    private Long topicId;

    /**
     * 话题名称
     */
    @TableField(value = "topic_name")
    private String topicName;

    /**
     * 是否置顶(0：未置顶 1：置顶)
     */
    @TableField(value = "is_top")
    private Boolean isTop;

    /**
     * 类型(0：图文 1：视频)
     */
    @TableField(value = "`type`")
    private Integer type;

    /**
     * 笔记图片链接(逗号隔开)
     */
    @TableField(value = "img_uris")
    private String imgUris;

    /**
     * 视频链接
     */
    @TableField(value = "video_uri")
    private String videoUri;

    /**
     * 可见范围(0：公开,所有人可见 1：仅对自己可见)
     */
    @TableField(value = "visible")
    private Integer visible;

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
     * 状态(0：待审核 1：正常展示 2：被删除(逻辑删除) 3：被下架)
     */
    @TableField(value = "`status`")
    private Integer status;

    /**
     * 笔记内容UUID
     */
    @TableField(value = "content_uuid")
    private String contentUuid;
}