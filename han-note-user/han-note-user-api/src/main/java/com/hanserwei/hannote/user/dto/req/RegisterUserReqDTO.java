package com.hanserwei.hannote.user.dto.req;

import com.hanserwei.framework.common.validate.EmailNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterUserReqDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "邮箱号不能为空")
    @EmailNumber
    private String email;

}