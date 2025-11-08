package com.hanserwei.hannote.comment.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import com.hanserwei.hannote.comment.biz.model.bo.CommentBO;
import com.hanserwei.hannote.comment.biz.model.bo.CommentHeatBO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentDOMapper extends BaseMapper<CommentDO> {
    /**
     * 根据评论 ID 批量查询
     *
     * @param commentIds 评论 ID 列表
     * @return 评论列表
     */
    List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 批量插入评论
     *
     * @param comments 评论列表
     * @return 插入数量
     */
    int batchInsert(@Param("comments") List<CommentBO> comments);

    /**
     * 批量更新热度值
     *
     * @param commentIds     评论 ID 列表
     * @param commentHeatBOS 热度值列表
     * @return 更新数量
     */
    int batchUpdateHeatByCommentIds(@Param("commentIds") List<Long> commentIds,
                                    @Param("commentHeatBOS") List<CommentHeatBO> commentHeatBOS);

    /**
     * 查询一级评论下最早回复的评论
     *
     * @param parentId 一级评论 ID
     * @return 一级评论下最早回复的评论
     */
    CommentDO selectEarliestByParentId(Long parentId);

    /**
     * 更新一级评论的 first_reply_comment_id
     *
     * @param firstReplyCommentId 一级评论下最早回复的评论 ID
     * @param id                  一级评论 ID
     * @return 更新数量
     */
    int updateFirstReplyCommentIdByPrimaryKey(@Param("firstReplyCommentId") Long firstReplyCommentId,
                                              @Param("id") Long id);

    /**
     * 查询评论分页数据
     *
     * @param noteId   笔记 ID
     * @param offset   偏移量
     * @param pageSize 页大小
     * @return 评论分页数据
     */
    List<CommentDO> selectPageList(@Param("noteId") Long noteId,
                                   @Param("offset") long offset,
                                   @Param("pageSize") long pageSize);

    /**
     * 批量查询二级评论
     *
     * @param commentIds 评论 ID 列表
     * @return 二级评论
     */
    List<CommentDO> selectTwoLevelCommentByIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 查询热门评论
     *
     * @param noteId 笔记 ID
     * @return 热门评论
     */
    List<CommentDO> selectHeatComments(Long noteId);
}