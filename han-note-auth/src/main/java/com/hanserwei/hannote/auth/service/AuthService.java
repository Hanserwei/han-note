package com.hanserwei.hannote.auth.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.hannote.auth.model.vo.user.UserLoginReqVO;

public interface AuthService  {

    /**
     * 登录与注册
     *
     * @param reqVO 请求参数
     * @return 响应结果
     */
    Response<String> loginAndRegister(UserLoginReqVO reqVO);

    /**
     * 退出登录
     * @return 响应结果
     */
    Response<?> logout();

    /**
     * 修改密码
     * @param updatePasswordReqVO 请求参数
     * @return 响应结果
     */
    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}
