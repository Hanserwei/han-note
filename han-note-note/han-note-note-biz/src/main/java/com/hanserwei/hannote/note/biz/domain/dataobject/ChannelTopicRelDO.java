package com.hanserwei.hannote.note.biz.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 频道-话题关联表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_channel_topic_rel")
public class ChannelTopicRelDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 频道ID
     */
    @TableField(value = "channel_id")
    private Long channelId;

    /**
     * 话题ID
     */
    @TableField(value = "topic_id")
    private Long topicId;

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
}