package com.hanserwei.hannote.auth.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.auth.model.vo.SendVerificationCodeReqVO;

public interface VerificationCodeService {

    /**
     * 发送短信验证码
     *
     * @param sendVerificationCodeReqVO 发送验证码VO
     * @return 返回响应
     */
    Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}