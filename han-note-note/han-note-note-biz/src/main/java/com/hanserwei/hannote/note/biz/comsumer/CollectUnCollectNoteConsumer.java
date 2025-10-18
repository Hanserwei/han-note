package com.hanserwei.hannote.note.biz.comsumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.note.biz.constant.MQConstants;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteCollectionDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.hanserwei.hannote.note.biz.model.dto.CollectUnCollectNoteMqDTO;
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
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "han_note_" + MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
        topic = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
        consumeMode = ConsumeMode.ORDERLY
)
public class CollectUnCollectNoteConsumer implements RocketMQListener<Message> {

    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        // 幂等性: 通过联合唯一索引保证

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> CollectUnCollectNoteConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_COLLECT)) { // 收藏笔记
            handleCollectNoteTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UN_COLLECT)) { // 取消收藏笔记
            handleUnCollectNoteTagMessage(bodyJsonStr);
        }
    }

    /**
     * 处理取消收藏笔记的 MQ 消息
     *
     * @param bodyJsonStr 消息体
     */
    private void handleUnCollectNoteTagMessage(String bodyJsonStr) {

    }

    /**
     * 处理收藏笔记的 MQ 消息
     *
     * @param bodyJsonStr 消息体
     */
    private void handleCollectNoteTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = JsonUtils.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);

        if (Objects.isNull(collectUnCollectNoteMqDTO)) return;

        // 用户ID
        Long userId = collectUnCollectNoteMqDTO.getUserId();
        // 收藏的笔记ID
        Long noteId = collectUnCollectNoteMqDTO.getNoteId();
        // 操作类型
        Integer type = collectUnCollectNoteMqDTO.getType();
        // 收藏时间
        LocalDateTime createTime = collectUnCollectNoteMqDTO.getCreateTime();

        // 构建 DO 对象
        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(userId)
                .noteId(noteId)
                .createTime(createTime)
                .status(type)
                .build();

        // 添加或更新笔记收藏记录
        boolean isSuccess = noteCollectionDOMapper.insertOrUpdate(noteCollectionDO);

        // TODO: 发送计数 MQ
    }
}
