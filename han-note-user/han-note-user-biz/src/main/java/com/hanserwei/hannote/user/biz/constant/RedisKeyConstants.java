package com.hanserwei.hannote.user.biz.constant;

public class RedisKeyConstants {

    /**
     * 小憨书全局 ID 生成器 KEY
     */
    public static final String HAN_NOTE_ID_GENERATOR_KEY = "hannote.id.generator";

    /**
     * 用户角色数据 KEY 前缀
     */
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

    /**
     * 角色对应的权限集合 KEY 前缀
     */
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";

    /**
     * 用户信息数据 KEY 前缀
     */
    private static final String USER_INFO_KEY_PREFIX = "user:info:";

    /**
     * 构建用户-角色 Key
     *
     * @param userId 邮箱
     * @return 用户角色key
     */
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLES_KEY_PREFIX + userId;
    }

    /**
     * 构建角色对应的权限集合 KEY
     *
     * @param roleKey 角色ID
     * @return 角色权限集合key
     */
    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }

    /**
     * 构建角色对应的权限集合 KEY
     * @param userId 用户ID
     * @return 用户信息key
     */
    public static String buildUserInfoKey(Long userId) {
        return USER_INFO_KEY_PREFIX + userId;
    }
}