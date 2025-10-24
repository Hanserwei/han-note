package com.hanserwei.hannote.data.align.consumer;

import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.data.align.constant.MQConstants;
import com.hanserwei.hannote.data.align.constant.RedisKeyConstants;
import com.hanserwei.hannote.data.align.constant.TableConstants;
import com.hanserwei.hannote.data.align.domain.mapper.InsertMapper;
import com.hanserwei.hannote.data.align.model.vo.CollectUnCollectNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "han_note_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String body) {
        log.info("## TodayNoteCollectIncrementData2DBConsumer 消费到了 MQ: {}", body);
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        // 消息体转DTO
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = JsonUtils.parseObject(body, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(collectUnCollectNoteMqDTO)) {
            return;
        }
        log.info("## TodayNoteCollectIncrementData2DBConsumer 笔记收藏数据：{}", JsonUtils.toJsonString(collectUnCollectNoteMqDTO));

        // 被收藏的笔记ID
        Long noteId = collectUnCollectNoteMqDTO.getNoteId();
        // 笔记作者ID
        Long noteCreatorId = collectUnCollectNoteMqDTO.getNoteCreatorId();
        // 今日日期
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ------------------------- 笔记的收藏数变更记录 -------------------------

        // 笔记对应的 Bloom Key
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteCollectNoteIdListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_collect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(noteBloomKey), noteId);

        // Lua 脚本：添加到布隆过滤器
        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库数据库

            // 根据分片总数，取模，获取对应的分片序号
            long noteIdHashKey = noteId % tableShards;

            try {
                insertMapper.insert2DataAlignNoteCollectCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
            } catch (Exception e) {
                log.error("## TodayNoteCollectIncrementData2DBConsumer 笔记收藏数变更记录失败：{}", e.getMessage());
            }

            // 数据库落库成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(noteBloomKey), noteId);
        }

        // ------------------------- 笔记作者的收藏数变更记录 -------------------------
        // 笔记作者对应的 Bloom Key
        String userBloomKey = RedisKeyConstants.buildBloomUserNoteCollectUserIdListKey(date);
        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), noteCreatorId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库数据库

            // 根据分片总数，取模，获取对应的分片序号
            long noteCreatorIdHashKey = noteCreatorId % tableShards;
            try {
                insertMapper.insert2DataAlignUserCollectCountTempTable(TableConstants.buildTableNameSuffix(date, noteCreatorIdHashKey), noteCreatorId);
            } catch (Exception e) {
                log.error("## TodayNoteCollectIncrementData2DBConsumer 笔记作者的收藏数变更记录失败：{}", e.getMessage());
            }
            // 数据库落库成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), noteCreatorId);
        }
    }
}
