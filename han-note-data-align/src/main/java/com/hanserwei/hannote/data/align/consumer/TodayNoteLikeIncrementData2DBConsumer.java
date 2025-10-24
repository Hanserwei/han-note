package com.hanserwei.hannote.data.align.consumer;

import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.data.align.constant.MQConstants;
import com.hanserwei.hannote.data.align.constant.RedisKeyConstants;
import com.hanserwei.hannote.data.align.constant.TableConstants;
import com.hanserwei.hannote.data.align.domain.mapper.InsertMapper;
import com.hanserwei.hannote.data.align.model.vo.LikeUnlikeNoteMqDTO;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "han_note_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private InsertMapper insertMapper;

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;
    @Override
    public void onMessage(String body) {
        log.info("## TodayNoteLikeIncrementData2DBConsumer 消费到了 MQ: {}", body);
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        // Json字符串转DTO
        LikeUnlikeNoteMqDTO noteLikeCountMqDTO = JsonUtils.parseObject(body, LikeUnlikeNoteMqDTO.class);
        if (Objects.isNull(noteLikeCountMqDTO)) {
            return;
        }
        log.info("## TodayNoteLikeIncrementData2DBConsumer 笔记点赞数据：{}", JsonUtils.toJsonString(noteLikeCountMqDTO));
        // 获取被点赞或者取消点赞的笔记ID
        Long noteId = noteLikeCountMqDTO.getNoteId();
        // 获取点赞或取消点赞的笔记的创建者ID
        Long noteCreatorId = noteLikeCountMqDTO.getNoteCreatorId();

        // 今日日期
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串

        String bloomKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_like_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteId);
        log.info("布隆过滤器判断结果：{}", result);

        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            // 根据分片总数，取模，分别获取对应的分片序号
            long userIdHashKey = noteCreatorId % tableShards;
            long noteIdHashKey = noteId % tableShards;
            log.info("根据分片总数，取模，分别获取对应的分片序号user:{},note:{}", userIdHashKey, noteIdHashKey);

            // 编程式事务，保证多语句的原子性
            transactionTemplate.execute(status -> {
                try {
                    // 将日增量变更数据，分别写入两张表
                    // - t_data_align_note_like_count_temp_日期_分片序号
                    // - t_data_align_user_like_count_temp_日期_分片序号
                    insertMapper.insert2DataAlignNoteLikeCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
                    insertMapper.insert2DataAlignUserLikeCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
                    return true;
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    log.error("## TodayNoteLikeIncrementData2DBConsumer 落库失败，回滚事务", ex);
                }
                return false;
            });
            // 3. 数据库写入成功后，再添加布隆过滤器中
            // 4. 数据库写入成功后，再添加布隆过滤器中
            RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
            redisTemplate.execute(bloomAddScript, Collections.singletonList(bloomKey), noteId);
        }
    }
}
