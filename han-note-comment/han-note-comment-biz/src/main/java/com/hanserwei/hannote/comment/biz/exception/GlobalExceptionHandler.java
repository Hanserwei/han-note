package com.hanserwei.hannote.comment.biz.exception;

import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.comment.biz.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@SuppressWarnings("LoggingSimilarMessage")
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常
     *
     * @return Response.fail(e)
     */
    @ExceptionHandler({ApiException.class})
    @ResponseBody
    public Response<Object> handleApiException(HttpServletRequest request, ApiException e) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMsg());
        return Response.fail(e);
    }

    /**
     * 捕获参数校验异常
     *
     * @return Response.fail(errorCode, errorMessage)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        // 参数错误异常码
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        // 获取 BindingResult
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb = new StringBuilder();

        // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确, 当前值: '123124qq.com';
        Optional.of(bindingResult.getFieldErrors()).ifPresent(errors -> {
            errors.forEach(error ->
                    sb.append(error.getField())
                            .append(" ")
                            .append(error.getDefaultMessage())
                            .append(", 当前值: '")
                            .append(error.getRejectedValue())
                            .append("'; ")

            );
        });

        // 错误信息
        String errorMessage = sb.toString();

        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        return Response.fail(errorCode, errorMessage);
    }

    /**
     * 捕获 guava 参数校验异常
     *
     * @return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID)
     */
    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public Response<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        // 参数错误异常码
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        // 错误信息
        String errorMessage = e.getMessage();

        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        return Response.fail(errorCode, errorMessage);
    }

    /**
     * 其他类型异常
     *
     * @param request 请求
     * @param e       异常
     * @return Response.fail(ResponseCodeEnum.SYSTEM_ERROR)
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error, ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}

