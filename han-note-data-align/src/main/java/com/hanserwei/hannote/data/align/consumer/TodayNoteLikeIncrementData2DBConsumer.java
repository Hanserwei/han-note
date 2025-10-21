package com.hanserwei.hannote.data.align.consumer;

import com.hanserwei.hannote.data.align.constant.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "han_note_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String body) {
        log.info("## TodayNoteLikeIncrementData2DBConsumer 消费到了 MQ: {}", body);
    }
}
