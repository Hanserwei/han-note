package com.hanserwei.hannote.auth.model.vo.verificationcode;

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
public class SendVerificationCodeReqVO {

    @NotBlank(message = "邮箱不能为空")
    @EmailNumber
    private String email;

}