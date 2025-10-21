package com.hanserwei.hannote.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 删除表
 */
public interface DeleteTableMapper {

    /**
     * 删除日增量表：关注数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：粉丝数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：笔记收藏数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：用户被收藏数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：用户被点赞数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：笔记点赞数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);

    /**
     * 删除日增量表：笔记发布数计数变更
     *
     * @param tableNameSuffix 表名后缀
     */
    void deleteDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix);
}