package com.hanserwei.hannote.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 查询
 */
public interface SelectRecordMapper {


    /**
     * 日增量表：关注数计数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                               @Param("batchSize") int batchSize);

    /**
     * 查询 t_following 关注表，获取关注总数
     *
     * @param userId 用户 ID
     * @return 关注总数
     */
    int selectCountFromFollowingTableByUserId(long userId);
}