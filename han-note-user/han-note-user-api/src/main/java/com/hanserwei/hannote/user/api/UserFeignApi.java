package com.hanserwei.hannote.user.api;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.constant.ApiConstants;
import com.hanserwei.hannote.user.dto.req.*;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    /**
     * 用户注册
     *
     * @param registerUserReqDTO 注册信息
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/register")
    Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据邮箱号查询用户信息
     *
     * @param findUserByEmailReqDTO 查询信息请求
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/findByEmail")
    Response<FindUserByEmailRspDTO> findByEmail(@RequestBody FindUserByEmailReqDTO findUserByEmailReqDTO);

    /**
     * 更新密码
     *
     * @param updateUserPasswordReqDTO 修改密码请求
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param findUserByIdReqDTO 查询信息请求
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/findById")
    Response<FindUserByIdRspDTO> findById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 批量查询用户信息
     *
     * @param findUsersByIdsReqDTO 批量查询信息请求
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/findByIds")
    Response<List<FindUserByIdRspDTO>> findByIds(@RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}