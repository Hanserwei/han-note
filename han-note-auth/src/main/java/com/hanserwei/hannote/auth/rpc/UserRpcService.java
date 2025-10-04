package com.hanserwei.hannote.auth.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.api.UserFeignApi;
import com.hanserwei.hannote.user.dto.req.FindUserByEmailReqDTO;
import com.hanserwei.hannote.user.dto.req.RegisterUserReqDTO;
import com.hanserwei.hannote.user.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 用户注册
     *
     * @param email 邮箱
     * @return 用户ID
     */
    public Long registerUser(String email) {
        RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
        registerUserReqDTO.setEmail(email);

        Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);

        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

    /**
     * 根据邮箱号查询用户信息
     *
     * @param email 邮箱
     * @return 用户信息
     */
    public FindUserByEmailRspDTO findUserByEmail(String email) {
        FindUserByEmailReqDTO findUserByEmailReqDTO = new FindUserByEmailReqDTO();
        findUserByEmailReqDTO.setEmail(email);

        Response<FindUserByEmailRspDTO> response = userFeignApi.findByPhone(findUserByEmailReqDTO);

        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

    /**
     * 密码更新
     *
     * @param encodePassword 加密后的密码
     */
    public void updatePassword(String encodePassword) {
        UpdateUserPasswordReqDTO updateUserPasswordReqDTO = new UpdateUserPasswordReqDTO();
        updateUserPasswordReqDTO.setEncodePassword(encodePassword);

        userFeignApi.updatePassword(updateUserPasswordReqDTO);
    }

}