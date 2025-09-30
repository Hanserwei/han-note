package com.hanserwei.framework.common.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailValidator implements ConstraintValidator<EmailNumber, String> {

    @Override
    public void initialize(EmailNumber constraintAnnotation) {
    }

    @Override
    public boolean isValid(String emailNumber, ConstraintValidatorContext constraintValidatorContext) {
        return emailNumber != null && emailNumber.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");
    }
}
