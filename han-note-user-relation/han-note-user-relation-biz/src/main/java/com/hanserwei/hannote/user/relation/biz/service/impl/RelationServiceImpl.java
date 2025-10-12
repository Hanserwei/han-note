package com.hanserwei.hannote.user.relation.biz.service.impl;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.hannote.user.relation.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.user.relation.biz.model.vo.FollowUserReqVO;
import com.hanserwei.hannote.user.relation.biz.rpc.UserRpcService;
import com.hanserwei.hannote.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private UserRpcService userRpcService;

    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 获取被关注用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();

        // 获取当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.equals(userId, followUserId)) {
            throw new ApiException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }
        // 校验关注的用户是否存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);
        if (Objects.isNull(findUserByIdRspDTO)){
            throw new ApiException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // TODO: 校验关注数是否已经达到上限

        // TODO: 写入 Redis ZSET 关注列表

        // TODO: 发送 MQ

        return Response.success();
    }
}
