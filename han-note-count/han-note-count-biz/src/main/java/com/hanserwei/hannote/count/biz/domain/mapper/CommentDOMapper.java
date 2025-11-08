package com.hanserwei.hannote.count.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.CommentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentDOMapper extends BaseMapper<CommentDO> {

    /**
     * 更新一级评论的子评论总数
     *
     * @param parentId 一级评论 ID
     * @param count    子评论数
     * @return 更新结果
     */
    int updateChildCommentTotal(@Param("parentId") Long parentId, @Param("count") int count);

    /**
     * 更新评论点赞数
     *
     * @param count     计数
     * @param commentId 评论 ID
     * @return 更新结果
     */
    int updateLikeTotalByCommentId(@Param("count") Integer count,
                                   @Param("commentId") Long commentId);
}