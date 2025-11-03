package com.hanserwei.hannote.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.hanserwei.hannote.data.align.constant.RedisKeyConstants;
import com.hanserwei.hannote.data.align.constant.TableConstants;
import com.hanserwei.hannote.data.align.domain.mapper.DeleteRecordMapper;
import com.hanserwei.hannote.data.align.domain.mapper.SelectRecordMapper;
import com.hanserwei.hannote.data.align.domain.mapper.UpdateRecordMapper;
import com.hanserwei.hannote.data.align.rpc.SearchRpcService;
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
public class FansCountShardingXxlJob {

    @Resource
    private SelectRecordMapper selectRecordMapper;
    @Resource
    private UpdateRecordMapper updateRecordMapper;
    @Resource
    private DeleteRecordMapper deleteRecordMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SearchRpcService searchRpcService;

    /**
     * 分片广播任务
     */
    @XxlJob("fansCountShardingJobHandler")
    public void fansCountShardingJobHandler() {
        // 获取分片参数
        // 分片序号
        int shardIndex = XxlJobHelper.getShardIndex();
        // 分片总数
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("=================> 开始定时分片广播任务：对当日发生变更的用户粉丝数进行对齐");
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
            // 分批次查询t_data_align_fans_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
            List<Long> userIds = selectRecordMapper.selectBatchFromDataAlignFansCountTempTable(tableNameSuffix, batchSize);

            // 若记录为空，则结束循环
            if (CollUtil.isEmpty(userIds)) {
                break;
            }

            // 循环这一批发生变化的用户ID
            userIds.forEach(userId -> {
                // 1. 对t_fans进行count(*)操作，获取该用户ID的粉丝总数
                int fansTotal = selectRecordMapper.selectCountFromFansTableByUserId(userId);
                // 2. 更新t_user_count的粉丝总数
                int count = updateRecordMapper.updateUserFansTotalByUserId(userId, fansTotal);
                // 更新Redis缓存
                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountUserKey(userId);
                    boolean hashKey = redisTemplate.hasKey(redisKey);
                    if (hashKey) {
                        redisTemplate.opsForHash().put(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, fansTotal);
                    }
                }
                // 远程 RPC, 调用搜索服务，重新构建索引
                searchRpcService.rebuildUserDocument(userId);
            });

            // 删除t_data_align_fans_count_temp_日期_分片序号中的记录
            deleteRecordMapper.batchDeleteDataAlignFansCountTempTable(tableNameSuffix, userIds);

            // 当前已处理的记录数
            processedTotal += userIds.size();
        }

        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的用户粉丝数进行对齐，共处理 {} 条记录", processedTotal);
    }

}