package com.hanserwei.framework.common.utils;

import java.util.regex.Pattern;

public final class ParamUtils {
    private ParamUtils() {
    }

    // ============================== 校验昵称 ==============================
    // 定义昵称长度范围
    private static final int NICK_NAME_MIN_LENGTH = 2;
    private static final int NICK_NAME_MAX_LENGTH = 24;

    // 定义特殊字符的正则表达式
    private static final String NICK_NAME_REGEX = "[!@#$%^&*(),.?\":{}|<>]";

    /**
     * 昵称校验
     * 
     * @param nickname 昵称
     * @return  boolean
     */
    public static boolean checkNickname(String nickname) {
        // 检查长度
        if (nickname.length() < NICK_NAME_MIN_LENGTH || nickname.length() > NICK_NAME_MAX_LENGTH) {
            return false;
        }

        // 检查是否含有特殊字符
        Pattern pattern = Pattern.compile(NICK_NAME_REGEX);
        return !pattern.matcher(nickname).find();
    }

    // ============================== 校验小憨书号 ==============================
    // 定义 ID 长度范围
    private static final int ID_MIN_LENGTH = 6;
    private static final int ID_MAX_LENGTH = 15;

    // 定义正则表达式
    private static final String ID_REGEX = "^[a-zA-Z0-9_]+$";

    /**
     * 小憨书 ID 校验
     * 
     * @param hannoteId 小憨书 ID
     * @return  boolean
     */
    public static boolean checkHannoteId(String hannoteId) {
        // 检查长度
        if (hannoteId.length() < ID_MIN_LENGTH || hannoteId.length() > ID_MAX_LENGTH) {
            return false;
        }
        // 检查格式
        Pattern pattern = Pattern.compile(ID_REGEX);
        return pattern.matcher(hannoteId).matches();
    }

    /**
     * 字符串长度校验
     * 
     * @param str 字符串
     * @param length 长度
     * @return  boolean
     */
    public static boolean checkLength(String str, int length) {
        // 检查长度
        return !str.isEmpty() && str.length() <= length;
    }
}