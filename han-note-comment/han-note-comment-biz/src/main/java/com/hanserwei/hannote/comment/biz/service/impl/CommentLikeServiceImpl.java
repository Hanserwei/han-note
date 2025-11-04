package com.hanserwei.hannote.comment.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentLikeDO;
import com.hanserwei.hannote.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.hanserwei.hannote.comment.biz.service.CommentLikeService;
import org.springframework.stereotype.Service;

@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeDOMapper, CommentLikeDO> implements CommentLikeService {

}
