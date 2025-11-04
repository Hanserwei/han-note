package com.hanserwei.hannote.comment.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentDOMapper extends BaseMapper<CommentDO> {
}