package com.hanserwei.hannote.user.api;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.constant.ApiConstants;
import com.hanserwei.hannote.user.dto.req.FindUserByEmailReqDTO;
import com.hanserwei.hannote.user.dto.req.FindUserByIdReqDTO;
import com.hanserwei.hannote.user.dto.req.RegisterUserReqDTO;
import com.hanserwei.hannote.user.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     * 根据手机号查询用户信息
     *
     * @param findUserByEmailReqDTO 查询信息请求
     * @return 响应
     */
    @PostMapping(value = PREFIX + "/findByEmail")
    Response<FindUserByEmailRspDTO> findByPhone(@RequestBody FindUserByEmailReqDTO findUserByEmailReqDTO);

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
}