package com.hanserwei.hannote.note.biz.comsumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.note.biz.constant.MQConstants;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteLikeDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteLikeDOMapper;
import com.hanserwei.hannote.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@Component
@RocketMQMessageListener(
        consumerGroup = "han_note_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
        consumeMode = ConsumeMode.ORDERLY// 顺序消费
)
@Slf4j
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {

    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        // 幂等性，通过联合索引保证

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_LIKE)) { // 点赞笔记
            handleLikeNoteTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNLIKE)) { // 取消点赞笔记
            handleUnlikeNoteTagMessage(bodyJsonStr);
        }
    }

    /**
     * 处理取消点赞笔记的 MQ 消息
     *
     * @param bodyJsonStr 消息体
     */
    private void handleUnlikeNoteTagMessage(String bodyJsonStr) {

    }

    /**
     * 处理点赞笔记的 MQ 消息
     *
     * @param bodyJsonStr 消息体
     */
    private void handleLikeNoteTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        LikeUnlikeNoteMqDTO likeNoteMqDTO = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);

        if (Objects.isNull(likeNoteMqDTO)) return;

        // 用户ID
        Long userId = likeNoteMqDTO.getUserId();
        // 点赞的笔记ID
        Long noteId = likeNoteMqDTO.getNoteId();
        // 操作类型
        Integer type = likeNoteMqDTO.getType();
        // 点赞时间
        LocalDateTime createTime = likeNoteMqDTO.getCreateTime();

        // 构建 DO 对象
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .createTime(createTime)
                .status(type)
                .build();

        // 添加或更新笔记点赞记录
        boolean count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);

        // TODO: 发送计数 MQ

    }
}
