package com.hanserwei.hannote.auth.model.vo.user;

import com.hanserwei.framework.common.validate.EmailNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginReqVO {

    /**
     * 邮箱号
     */
    @NotBlank(message = "邮箱不能为空")
    @EmailNumber
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 密码
     */
    private String password;

    /**
     * 登录类型：邮箱验证码，或者是账号密码
     */
    @NotNull(message = "登录类型不能为空")
    private Integer type;
}