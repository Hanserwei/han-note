package com.hanserwei.hannote.user.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.biz.domain.dataobject.UserDO;
import com.hanserwei.hannote.user.biz.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.hannote.user.dto.req.FindUserByEmailReqDTO;
import com.hanserwei.hannote.user.dto.req.RegisterUserReqDTO;
import com.hanserwei.hannote.user.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;

public interface UserService extends IService<UserDO> {

    /**
     * 更新用户信息
     *
     * @param updateUserInfoReqVO 更新用户信息请求参数
     * @return 响应结果
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

    /**
     * 用户注册
     *
     * @param registerUserReqDTO 注册用户请求参数
     * @return 响应结果
     */
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据邮箱号查询用户信息
     *
     * @param findUserByEmailReqDTO 查询用户信息请求参数
     * @return 响应结果
     */
    Response<FindUserByEmailRspDTO> findByEmail(FindUserByEmailReqDTO findUserByEmailReqDTO);

    /**
     * 更新密码
     *
     * @param updateUserPasswordReqDTO 更新密码请求参数
     * @return 响应结果
     */
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);
}