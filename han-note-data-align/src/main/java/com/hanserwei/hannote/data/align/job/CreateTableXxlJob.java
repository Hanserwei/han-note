package com.hanserwei.hannote.data.align.job;

import com.hanserwei.hannote.data.align.constant.TableConstants;
import com.hanserwei.hannote.data.align.domain.mapper.CreateTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RefreshScope
@SuppressWarnings("unused")
public class CreateTableXxlJob {

    @Resource
    private CreateTableMapper createTableMapper;

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @SuppressWarnings("unused")
    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() {
        XxlJobHelper.log("## 开始初始化明日增量数据表...");
        String date = LocalDate.now().plusDays(1) // 明日的日期
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        XxlJobHelper.log("## 开始创建日增量数据表，日期: {}...", date);
        if (tableShards > 0) {
            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                // 表名后缀
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);

                // 创建表
                // 创建表
                createTableMapper.createDataAlignFollowingCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignFansCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 创建日增量数据表成功，表名后缀: {}...", date);
    }

}