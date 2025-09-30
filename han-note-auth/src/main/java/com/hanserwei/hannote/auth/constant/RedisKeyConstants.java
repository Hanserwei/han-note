package com.hanserwei.hannote.auth.constant;

public class RedisKeyConstants {

    /**
     * 验证码 KEY 前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    /**
     * 构建验证码 KEY
     * @param email 手机号
     * @return 验证码key
     */
    public static String buildVerificationCodeKey(String email) {
        return VERIFICATION_CODE_KEY_PREFIX + email;
    }
}