package com.hanserwei.hannote.comment.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import com.hanserwei.hannote.comment.biz.model.bo.CommentBO;
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
}