package com.hanserwei.hannote.search.canal;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Maps;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.hannote.search.domain.mapper.SelectMapper;
import com.hanserwei.hannote.search.enums.NoteStatusEnum;
import com.hanserwei.hannote.search.enums.NoteVisibleEnum;
import com.hanserwei.hannote.search.index.NoteIndex;
import com.hanserwei.hannote.search.index.UserIndex;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CanalSchedule implements Runnable {

    @Resource
    private CanalProperties canalProperties;
    @Resource
    private CanalConnector canalConnector;
    @Resource
    private SelectMapper selectMapper;
    @Resource
    private ElasticsearchClient client;

    @Override
    @Scheduled(fixedDelay = 100) // 每隔 100ms 被执行一次
    public void run() {
        // 初始化批次 ID，-1 表示未开始或未获取到数据
        long batchId = -1;
        try {
            // 从 canalConnector 获取批量消息，返回的数据量由 batchSize 控制，若不足，则拉取已有的
            Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());

            // 获取当前拉取消息的批次 ID
            batchId = message.getId();

            // 获取当前批次中的数据条数
            long size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                try {
                    // 拉取数据为空，休眠 1s, 防止频繁拉取
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error("休眠异常", e);
                }
            } else {
                // 如果当前批次有数据，处理这批次数据
                processEntry(message.getEntries());
            }

            // 对当前批次的消息进行 ack 确认，表示该批次的数据已经被成功消费
            canalConnector.ack(batchId);
        } catch (Exception e) {
            log.error("消费 Canal 批次数据异常", e);
            // 如果出现异常，需要进行数据回滚，以便重新消费这批次的数据
            canalConnector.rollback(batchId);
        }
    }

    /**
     * 处理这一批次数据
     * @param entrys 批次数据
     */
    private void processEntry(List<CanalEntry.Entry> entrys) throws Exception {
        // 循环处理批次数据
        for (CanalEntry.Entry entry : entrys) {
            // 只处理 ROWDATA 行数据类型的 Entry，忽略事务等其他类型
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                // 获取事件类型（如：INSERT、UPDATE、DELETE 等等）
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                // 获取数据库名称
                String database = entry.getHeader().getSchemaName();
                // 获取表名称
                String table = entry.getHeader().getTableName();

                // 解析出 RowChange 对象，包含 RowData 和事件相关信息
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                // 遍历所有行数据（RowData）
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // 获取行中所有列的最新值（AfterColumns）
                    List<CanalEntry.Column> columns = rowData.getAfterColumnsList();

                    // 将列数据解析为 Map，方便后续处理
                    Map<String, Object> columnMap = parseColumns2Map(columns);

                    // 自定义处理
                    log.info("EventType: {}, Database: {}, Table: {}, Columns: {}", eventType, database, table, columnMap);
                    // 处理事件
                    processEvent(columnMap, table, eventType);
                }
            }
        }
    }

    private void processEvent(Map<String, Object> columnMap, String table, CanalEntry.EventType eventType) {
        switch (table) {
            case "t_note" -> handleNoteEvent(columnMap, eventType); // 笔记表
            case "t_user" -> handleUserEvent(columnMap, eventType); // 用户表
            default -> log.warn("Table: {} not support", table);
        }
    }

    /**
     * 处理用户表事件
     *
     * @param columnMap 列数据
     * @param eventType 事件类型
     */
    private void handleUserEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) {
        // 获取用户 ID
        Long userId = Long.parseLong(columnMap.get("id").toString());

        // 不同的事件，处理逻辑不同
        switch (eventType) {
            case INSERT -> syncUserIndex(userId); // 记录新增事件
            case UPDATE -> { // 记录更新事件
                // 用户变更后的状态
                Integer status = Integer.parseInt(columnMap.get("status").toString());
                // 逻辑删除
                Integer isDeleted = Integer.parseInt(columnMap.get("is_deleted").toString());

                if (Objects.equals(status, StatusEnum.ENABLE.getValue())
                        && Objects.equals(isDeleted, 0)) { // 用户状态为已启用，并且未被逻辑删除
                    // 更新用户索引、笔记索引
                    syncNotesIndexAndUserIndex(userId);
                } else if (Objects.equals(status, StatusEnum.DISABLED.getValue()) // 用户状态为禁用
                        || Objects.equals(isDeleted, 1)) { // 被逻辑删除
                    // 删除用户文档
                    deleteUserDocument(String.valueOf(userId));
                }
            }
            default -> log.warn("Unhandled event type for t_user: {}", eventType);
        }
    }

    /**
     * 删除用户文档
     *
     * @param documentId 文档 ID
     */
    private void deleteUserDocument(String documentId) {
        try {
            client.delete(d -> d.index(UserIndex.NAME).id(documentId));
        } catch (IOException e) {
            log.error("删除用户文档异常", e);
        }
    }

    /**
     * 同步用户索引、笔记索引（可能是多条）
     *
     * @param userId 用户 ID
     */
    private void syncNotesIndexAndUserIndex(Long userId) {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        // 1. 用户索引
        List<Map<String, Object>> userResult = selectMapper.selectEsUserIndexData(userId);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : userResult) {
            // 构建 BulkOperation
            BulkOperation op = BulkOperation.of(b -> b
                    .index(i -> i
                            .index(UserIndex.NAME)
                            .id(String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID)))
                            .document(recordMap)
                    )
            );
            // 通过 operations() 方法添加到 Builder
            bulkRequestBuilder.operations(op);
        }
        List<Map<String, Object>> noteResult = selectMapper.selectEsNoteIndexData(null, userId);
        for (Map<String, Object> recordMap : noteResult) {
            BulkOperation op = BulkOperation.of(b -> b
                    .index(i -> i
                            .index(NoteIndex.NAME)
                            .id(String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID)))
                            .document(recordMap)
                    )
            );
            bulkRequestBuilder.operations(op);
        }

        // 构建 BulkRequest
        BulkRequest bulkRequest = bulkRequestBuilder.build();
        try {
            client.bulk(bulkRequest);
        } catch (IOException e) {
            log.error("执行批量请求出错:", e);
        }
    }

    /**
     * 同步用户索引数据
     *
     * @param userId 用户 ID
     */
    private void syncUserIndex(Long userId) {
        // 1. 同步用户索引
        List<Map<String, Object>> userResult = selectMapper.selectEsUserIndexData(userId);

        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : userResult) {
            // 创建索引请求对象，指定索引名称
            IndexRequest<Object> request = IndexRequest.of(indexRequest -> indexRequest
                    // 指定索引名称
                    .index(UserIndex.NAME)
                    // 设置文档的 ID，使用记录中的主键 “id” 字段值
                    .id(String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID)))
                    // 设置文档的内容，使用查询结果的记录数据
                    .document(recordMap));
            // 将数据写入 Elasticsearch 索引
            try {
                client.index(request);
            } catch (IOException e) {
                log.error("写入索引异常", e);
            }
        }
    }

    /**
     * 处理笔记表事件
     *
     * @param columnMap 列数据
     * @param eventType 事件类型
     */
    private void handleNoteEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) {
        // 获取笔记 ID
        Long noteId = Long.parseLong(columnMap.get("id").toString());

        // 不同的事件，处理逻辑不同
        switch (eventType) {
            case INSERT -> syncNoteIndex(noteId); // 记录新增事件
            case UPDATE -> {
                // 记录更新事件
                // 笔记变更后的状态
                Integer status = Integer.parseInt(columnMap.get("status").toString());
                // 笔记可见范围
                Integer visible = Integer.parseInt(columnMap.get("visible").toString());

                if (Objects.equals(status, NoteStatusEnum.NORMAL.getCode())
                        && Objects.equals(visible, NoteVisibleEnum.PUBLIC.getCode())) { // 正常展示，并且可见性为公开
                    // 对索引进行覆盖更新
                    syncNoteIndex(noteId);
                } else if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode()) // 仅对自己可见
                        || Objects.equals(status, NoteStatusEnum.DELETED.getCode())
                        || Objects.equals(status, NoteStatusEnum.DOWNED.getCode())) { // 被逻辑删除、被下架
                    // 删除笔记文档
                    deleteNoteDocument(String.valueOf(noteId));
                }
            }
            default -> log.warn("Unhandled event type for t_note: {}", eventType);
        }
    }

    /**
     * 删除笔记文档
     *
     * @param documentId 文档ID
     */
    private void deleteNoteDocument(String documentId) {
        try {
            client.delete(deleteRequest -> deleteRequest
                    .index(NoteIndex.NAME)
                    .id(documentId));
        } catch (IOException e) {
            log.error("删除笔记文档异常", e);
        }
    }

    /**
     * 同步笔记索引
     *
     * @param noteId 笔记ID
     */
    private void syncNoteIndex(Long noteId) {
        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectEsNoteIndexData(noteId, null);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest<Object> request = IndexRequest.of(indexRequest -> indexRequest
                    .index(NoteIndex.NAME)
                    // 设置文档的 ID，使用记录中的主键 “id” 字段值
                    .id(String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID)))
                    // 设置文档的内容，使用查询结果的记录数据
                    .document(recordMap));
            // 将数据写入 Elasticsearch 索引
            try {
                IndexResponse response = client.index(request);
            } catch (IOException e) {
                log.error("写入 Elasticsearch 索引异常", e);
            }
        }
    }


    /**
     * 将列数据解析为 Map
     *
     * @param columns 列数据
     * @return Map
     */
    private Map<String, Object> parseColumns2Map(List<CanalEntry.Column> columns) {
        Map<String, Object> map = Maps.newHashMap();
        columns.forEach(column -> {
            if (Objects.isNull(column)) return;
            map.put(column.getName(), column.getValue());
        });
        return map;
    }

}