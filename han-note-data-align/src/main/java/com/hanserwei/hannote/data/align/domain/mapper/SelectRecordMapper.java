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
     * 查询 t_fans 粉丝表，获取粉丝总数
     *
     * @param userId 用户 ID
     * @return 粉丝总数
     */
    int selectCountFromFansTableByUserId(long userId);

    /**
     * 日增量表：粉丝数计数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                          @Param("batchSize") int batchSize);

    /**
     * 查询 t_following 关注表，获取关注总数
     *
     * @param userId 用户 ID
     * @return 关注总数
     */
    int selectCountFromFollowingTableByUserId(long userId);

    /**
     * 日增量表：笔记发布数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                                 @Param("batchSize") int batchSize);

    /**
     * 批量查询 t_note 笔记表，获取发布总数
     *
     * @param userId 用户 ID
     * @return 发布总数
     */
    int selectCountFromNoteTableByUserId(long userId);


    /**
     * 日增量表：笔记点赞数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                              @Param("batchSize") int batchSize);

    /**
     * 查询 t_note_like 笔记点赞表，获取点赞总数
     *
     * @param noteId 笔记 ID
     * @return 点赞总数
     */
    int selectCountFromNoteLikeTableByNoteId(long noteId);

    /**
     * 日增量表：笔记收藏数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                                 @Param("batchSize") int batchSize);

    /**
     * 批量查询 t_note_collection 笔记收藏表，获取收藏总数
     *
     * @param noteId 笔记 ID
     * @return 收藏总数
     */
    int selectCountFromNoteCollectTableByNoteId(long noteId);


    /**
     * 日增量表：用户收藏数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                                 @Param("batchSize") int batchSize);

    /**
     * 批量查询 t_note_collection 笔记收藏表，获取收藏总数
     *
     * @param userId 用户 ID
     * @return 收藏总数
     */
    int selectUserCollectCountFromNoteCollectionTableByUserId(Long userId);

    /**
     * 日增量表：用户点赞数变更 - 批量查询
     *
     * @param tableNameSuffix 表名后缀
     * @param batchSize       批量大小
     * @return 批量查询结果
     */
    List<Long> selectBatchFromDataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                              @Param("batchSize") int batchSize);

    /**
     * 批量查询 t_note_like 笔记点赞表，获取点赞总数
     *
     * @param userId 用户 ID
     * @return 点赞总数
     */
    int selectUserLikeCountFromNoteLikeTableByUserId(Long userId);
}