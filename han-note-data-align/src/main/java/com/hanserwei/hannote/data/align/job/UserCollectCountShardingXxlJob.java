package com.hanserwei.hannote.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.hanserwei.hannote.data.align.constant.RedisKeyConstants;
import com.hanserwei.hannote.data.align.constant.TableConstants;
import com.hanserwei.hannote.data.align.domain.mapper.DeleteRecordMapper;
import com.hanserwei.hannote.data.align.domain.mapper.SelectRecordMapper;
import com.hanserwei.hannote.data.align.domain.mapper.UpdateRecordMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class UserCollectCountShardingXxlJob {

    @Resource
    private SelectRecordMapper selectRecordMapper;
    @Resource
    private UpdateRecordMapper updateRecordMapper;
    @Resource
    private DeleteRecordMapper deleteRecordMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @XxlJob("userCollectCountShardingJobHandler")
    public void userCollectCountShardingJobHandler() throws Exception {
        // 获取分片参数
        // 分片序号
        int shardIndex = XxlJobHelper.getShardIndex();
        // 分片总数
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("=================> 开始定时分片广播任务：对当日发生变更的用户收藏数进行对齐");
        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 表后缀
        String date = LocalDate.now().minusDays(1) // 昨日的日期
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串
        // 表名后缀
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

        // 一批次 1000 条
        int batchSize = 1000;
        // 共对齐了多少条记录，默认为 0
        int processedTotal = 0;

        while (true) {
            // 分批次查询t_data_align_user_collect_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
            List<Long> userIds = selectRecordMapper.selectBatchFromDataAlignUserCollectCountTempTable(tableNameSuffix, batchSize);

            // 若记录为空，则结束循环
            if (CollUtil.isEmpty(userIds)) {
                break;
            }

            userIds.forEach(userId -> {
                //2: 对 t_user_collection 用户收藏表执行 count(*) 操作，获取用户获得的收藏总数
                int userCollectTotal = selectRecordMapper.selectUserCollectCountFromNoteCollectionTableByUserId(userId);

                // 3: 更新 t_user_count 用户计数表
                int count = updateRecordMapper.updateUserCollectTotalByUserId(userId, userCollectTotal);

                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountUserKey(userId);
                    boolean hashKey = redisTemplate.hasKey(redisKey);
                    if (hashKey) {
                        redisTemplate.opsForHash().put(redisKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, userCollectTotal);
                    }
                }
            });

            // 4: 删除 t_data_align_user_collect_count_temp_日期_分片序号
            deleteRecordMapper.batchDeleteDataAlignUserCollectCountTempTable(tableNameSuffix, userIds);

            processedTotal += userIds.size();
        }
        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的用户收藏数进行对齐，共处理 {} 条记录", processedTotal);
    }
}
