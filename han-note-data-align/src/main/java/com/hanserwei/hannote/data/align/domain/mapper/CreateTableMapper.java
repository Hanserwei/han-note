package com.hanserwei.hannote.data.align.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 自动创建表
 */
@Mapper
public interface CreateTableMapper {

    /**
     * 创建日增量表：关注数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：粉丝数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：笔记收藏数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：用户被收藏数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：用户被点赞数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：笔记点赞数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 创建日增量表：笔记发布数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void createDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);
}