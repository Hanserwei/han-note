package com.hanserwei.hannote.comment.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentLikeDO;
import com.hanserwei.hannote.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentLikeDOMapper extends BaseMapper<CommentLikeDO> {

    /**
     * 查询某个评论是否被点赞
     *
     * @param userId    用户 ID
     * @param commentId 评论 ID
     * @return 1 表示已点赞，0 表示未点赞
     */
    int selectCountByUserIdAndCommentId(@Param("userId") Long userId,
                                        @Param("commentId") Long commentId);

    /**
     * 查询对应用户点赞的所有评论
     *
     * @param userId 用户 ID
     * @return 评论点赞列表
     */
    List<CommentLikeDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 批量删除点赞记录
     *
     * @param unlikes 删除点赞记录
     * @return 删除数量
     */
    int batchDelete(@Param("unlikes") List<LikeUnlikeCommentMqDTO> unlikes);

    /**
     * 批量添加点赞记录
     *
     * @param likes 添加点赞记录
     * @return 添加数量
     */
    int batchInsert(@Param("likes") List<LikeUnlikeCommentMqDTO> likes);
}