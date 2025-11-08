package com.hanserwei.hannote.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.constant.DateConstants;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.DateUtils;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.comment.biz.constants.MQConstants;
import com.hanserwei.hannote.comment.biz.domain.dataobject.CommentDO;
import com.hanserwei.hannote.comment.biz.domain.mapper.CommentDOMapper;
import com.hanserwei.hannote.comment.biz.domain.mapper.NoteCountDOMapper;
import com.hanserwei.hannote.comment.biz.model.dto.PublishCommentMqDTO;
import com.hanserwei.hannote.comment.biz.model.vo.FindCommentItemRspVO;
import com.hanserwei.hannote.comment.biz.model.vo.FindCommentPageListReqVO;
import com.hanserwei.hannote.comment.biz.model.vo.PublishCommentReqVO;
import com.hanserwei.hannote.comment.biz.retry.SendMqRetryHelper;
import com.hanserwei.hannote.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.hanserwei.hannote.comment.biz.rpc.KeyValueRpcService;
import com.hanserwei.hannote.comment.biz.rpc.UserRpcService;
import com.hanserwei.hannote.comment.biz.service.CommentService;
import com.hanserwei.hannote.kv.dto.req.FindCommentContentReqDTO;
import com.hanserwei.hannote.kv.dto.resp.FindCommentContentRspDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentDOMapper, CommentDO> implements CommentService {

    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private UserRpcService userRpcService;

    @Override
    public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
        // 评论正文
        String content = publishCommentReqVO.getContent();
        // 附近图片
        String imageUrl = publishCommentReqVO.getImageUrl();

        // 评论内容和图片不能同时为空
        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        // 发布者ID
        Long creatorId = LoginUserContextHolder.getUserId();

        // RPC: 调用分布式 ID 生成服务，生成评论 ID
        String commentId = distributedIdGeneratorRpcService.generateCommentId();

        // 发送消息
        // 构造MQ消息体
        PublishCommentMqDTO publishCommentMqDTO = PublishCommentMqDTO.builder()
                .noteId(publishCommentReqVO.getNoteId())
                .content(content)
                .imageUrl(imageUrl)
                .replyCommentId(publishCommentReqVO.getReplyCommentId())
                .createTime(LocalDateTime.now())
                .creatorId(creatorId)
                .commentId(Long.valueOf(commentId))
                .build();
        // 发送 MQ 消息，包含重试机制
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(publishCommentMqDTO));
        return Response.success();
    }

    /**
     * 设置评论内容
     *
     * @param commentUuidAndContentMap 评论内容
     * @param commentDO                评论DO
     * @param firstReplyCommentRspVO   一级评论
     */
    private static void setCommentContent(Map<String, String> commentUuidAndContentMap, CommentDO commentDO, FindCommentItemRspVO firstReplyCommentRspVO) {
        if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
            String contentUuid = commentDO.getContentUuid();
            if (StringUtils.isNotBlank(contentUuid)) {
                firstReplyCommentRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
            }
        }
    }

    /**
     * 设置用户信息
     *
     * @param userIdAndDTOMap      用户信息
     * @param userId               用户ID
     * @param oneLevelCommentRspVO 一级评论
     */
    private static void setUserInfo(Map<Long, FindUserByIdRspDTO> userIdAndDTOMap, Long userId, FindCommentItemRspVO oneLevelCommentRspVO) {
        FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
        if (Objects.nonNull(findUserByIdRspDTO)) {
            oneLevelCommentRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
            oneLevelCommentRspVO.setNickname(findUserByIdRspDTO.getNickName());
        }
    }

    @Override
    public PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO findCommentPageListReqVO) {
        // 笔记ID
        Long noteId = findCommentPageListReqVO.getNoteId();
        //当前页码
        Integer pageNo = findCommentPageListReqVO.getPageNo();
        // 每页展示一级评论数量
        int pageSize = 10;

        // TODO： 先从缓存中查询

        // 查询评论总数
        Long count = noteCountDOMapper.selectCommentTotalByNoteId(noteId);

        if (Objects.isNull(count)) {
            return PageResponse.success(null, pageNo, pageSize);
        }

        // 分页返回参数
        List<FindCommentItemRspVO> commentRspVOS = null;

        // 若评论总数大于0
        if (count > 0) {
            commentRspVOS = Lists.newArrayList();
            // 计算分页查询的offset
            long offset = PageResponse.getOffset(pageNo, pageSize);
            //查询一级评论
            List<CommentDO> oneLevelCommentIds = commentDOMapper.selectPageList(noteId, offset, pageSize);
            // 过滤出所有最早回复的二级评论ID
            List<Long> twoLevelCommentIds = oneLevelCommentIds.stream()
                    .map(CommentDO::getFirstReplyCommentId)
                    .filter(e -> e != 0)
                    .toList();
            // 查询二级评论
            Map<Long, CommentDO> commentIdAndDOMap = null;
            List<CommentDO> twoLevelCommentDOS = null;
            if (CollUtil.isNotEmpty(twoLevelCommentIds)) {
                twoLevelCommentDOS = commentDOMapper.selectTwoLevelCommentByIds(twoLevelCommentIds);
                // 转Map方便后续数据拼接
                commentIdAndDOMap = twoLevelCommentDOS.stream()
                        .collect(Collectors.toMap(CommentDO::getId, e -> e));
            }

            // 调用KV服务需要的入参
            List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
            // 调用用户服务需要的入参
            List<Long> userIds = Lists.newArrayList();

            // 一二级评论合并到一起
            List<CommentDO> allCommentDOS = Lists.newArrayList();
            CollUtil.addAll(allCommentDOS, oneLevelCommentIds);
            CollUtil.addAll(allCommentDOS, twoLevelCommentDOS);

            // 循环提取RPC需要的入参数据
            allCommentDOS.forEach(commentDO -> {
                // 构建KV服务批量查询评论内容的入参
                boolean isContentEmpty = commentDO.getIsContentEmpty();
                if (!isContentEmpty) {
                    FindCommentContentReqDTO findCommentContentReqDTO = FindCommentContentReqDTO.builder()
                            .contentId(commentDO.getContentUuid())
                            .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(commentDO.getCreateTime()))
                            .build();
                    findCommentContentReqDTOS.add(findCommentContentReqDTO);
                }

                // 构建用户服务批量查询用户信息的入参
                userIds.add(commentDO.getUserId());
            });

            // RPC: 调用KV服务批量查询评论内容
            List<FindCommentContentRspDTO> findCommentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);
            // DTO转Map方便后续数据拼接
            Map<String, String> commentUuidAndContentMap = null;
            if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
                commentUuidAndContentMap = findCommentContentRspDTOS.stream()
                        .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, FindCommentContentRspDTO::getContent));
            }

            // RPC: 调用用户服务批量查询用户信息
            List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);
            // DTO转Map方便后续数据拼接
            Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
            if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
                userIdAndDTOMap = findUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, e -> e));
            }

            // DO转VO组装一二级评论数据
            for (CommentDO commentDO : oneLevelCommentIds) {
                // 一级评论
                Long userId = commentDO.getUserId();
                FindCommentItemRspVO oneLevelCommentRspVO = FindCommentItemRspVO.builder()
                        .userId(userId)
                        .commentId(commentDO.getId())
                        .imageUrl(commentDO.getImageUrl())
                        .createTime(DateUtils.formatRelativeTime(commentDO.getCreateTime()))
                        .likeTotal(commentDO.getLikeTotal())
                        .childCommentTotal(commentDO.getChildCommentTotal())
                        .build();
                // 用户信息
                if (userIdAndDTOMap != null) {
                    setUserInfo(userIdAndDTOMap, userId, oneLevelCommentRspVO);
                }
                // 笔记内容
                setCommentContent(commentUuidAndContentMap, commentDO, oneLevelCommentRspVO);

                // 二级评论
                Long firstReplyCommentId = commentDO.getFirstReplyCommentId();
                if (CollUtil.isNotEmpty(commentIdAndDOMap)) {
                    CommentDO firstReplyCommentDO = commentIdAndDOMap.get(firstReplyCommentId);
                    if (Objects.nonNull(firstReplyCommentDO)) {
                        Long firstReplyCommentUserId = firstReplyCommentDO.getUserId();
                        FindCommentItemRspVO firstReplyCommentRspVO = FindCommentItemRspVO.builder()
                                .userId(firstReplyCommentDO.getUserId())
                                .commentId(firstReplyCommentDO.getId())
                                .imageUrl(firstReplyCommentDO.getImageUrl())
                                .createTime(DateUtils.formatRelativeTime(firstReplyCommentDO.getCreateTime()))
                                .likeTotal(firstReplyCommentDO.getLikeTotal())
                                .build();
                        if (userIdAndDTOMap != null) {
                            setUserInfo(userIdAndDTOMap, firstReplyCommentUserId, firstReplyCommentRspVO);
                        }

                        // 用户信息
                        oneLevelCommentRspVO.setFirstReplyComment(firstReplyCommentRspVO);
                        // 笔记内容
                        setCommentContent(commentUuidAndContentMap, firstReplyCommentDO, firstReplyCommentRspVO);
                    }
                }
                commentRspVOS.add(oneLevelCommentRspVO);
            }
            // TODO 后续逻辑
        }
        return PageResponse.success(commentRspVOS, pageNo, count, pageSize);
    }
}
