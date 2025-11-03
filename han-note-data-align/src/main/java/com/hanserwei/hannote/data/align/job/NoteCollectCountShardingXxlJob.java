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
public class NoteCollectCountShardingXxlJob {

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

    @XxlJob("noteCollectCountShardingJobHandler")
    public void noteCollectCountShardingJobHandler() throws Exception {
        // 获取分片参数
        // 分片序号
        int shardIndex = XxlJobHelper.getShardIndex();
        // 分片总数
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("=================> 开始定时分片广播任务：对当日发生变更的笔记收藏数进行对齐");
        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 表后缀
        String date = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 表名后缀
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

        // 一批次1000条
        int batchSize = 1000;
        // 共对齐多少条数据，默认为0
        int processedTotal = 0;

        while (true) {
            // 1. 分批次查询 t_data_align_note_collect_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
            List<Long> noteIds = selectRecordMapper.selectBatchFromDataAlignNoteCollectCountTempTable(tableNameSuffix, batchSize);

            // 若查询结果为空，则跳出循环
            if (CollUtil.isEmpty(noteIds)) {
                break;
            }

            noteIds.forEach(noteId -> {
                int likeTotal = selectRecordMapper.selectCountFromNoteCollectTableByNoteId(noteId);

                // 3: 更新 t_note_count 表, 更新对应 Redis 缓存
                int count = updateRecordMapper.updateNoteCollectTotalByNoteId(noteId, likeTotal);
                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountNoteKey(noteId);
                    // 判断 Hash 是否存在
                    boolean hashKey = redisTemplate.hasKey(redisKey);
                    // 若存在
                    if (hashKey) {
                        // 更新对应 Redis 缓存
                        redisTemplate.opsForHash().put(redisKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, likeTotal);
                    }
                }
                // 远程 RPC, 调用搜索服务，重新构建文档
                searchRpcService.rebuildNoteDocument(noteId);
            });

            // 4. 批量物理删除这一批次记录
            deleteRecordMapper.batchDeleteDataAlignNoteCollectCountTempTable(tableNameSuffix, noteIds);

            processedTotal += noteIds.size();
        }

        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的笔记收藏数进行对齐，共对齐记录数：{}", processedTotal);
    }
}
