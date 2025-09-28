package com.hanserwei.framework.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException {
    // 异常码
    private String errorCode;
    // 异常信息
    private String errorMsg;

    public ApiException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMsg = baseExceptionInterface.getErrorMsg();
    }
}
