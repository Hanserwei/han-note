package com.hanserwei.hannote.auth.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("AUTH-10000", "出错啦，后台小维正在努力修复中..."),
    PARAM_NOT_VALID("AUTH-10001", "参数错误！！！"),

    // ----------- 业务异常状态码 -----------
    VERIFICATION_CODE_SEND_FREQUENTLY("AUTH-20000", "请求太频繁，请3分钟后再试"),
    MAIL_SEND_ERROR("AUTH-20001", "邮件发送失败，请稍后再试"),
    TEMPLATE_RENDER_ERROR("AUTH-20002", "模板渲染错误")
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMsg;

}

