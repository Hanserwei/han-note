package com.hanserwei.hannote.note.biz.comsumer;

import com.hanserwei.hannote.note.biz.constant.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "han_note_group_" + MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        messageModel = MessageModel.BROADCASTING
)
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String body) {
        Long noteId = Long.valueOf(body);
        log.info("## 消费者消费成功, noteId: {}", noteId);
    }
}
