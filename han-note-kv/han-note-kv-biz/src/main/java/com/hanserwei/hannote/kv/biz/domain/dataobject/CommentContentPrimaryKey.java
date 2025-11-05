package com.hanserwei.hannote.kv.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@PrimaryKeyClass
public class CommentContentPrimaryKey {

    /**
     * 分区键1
     */
    @PrimaryKeyColumn(name = "note_id", type = PrimaryKeyType.PARTITIONED)
    private Long noteId;

    /**
     * 分区键2
     */
    @PrimaryKeyColumn(name = "year_month", type = PrimaryKeyType.PARTITIONED)
    private String yearMonth;

    /**
     * 聚簇键
     */
    @PrimaryKeyColumn(name = "content_id", type = PrimaryKeyType.PARTITIONED)
    private UUID contentId;
}
