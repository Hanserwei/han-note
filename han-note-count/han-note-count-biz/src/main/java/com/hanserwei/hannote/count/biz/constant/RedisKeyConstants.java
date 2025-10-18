package com.hanserwei.hannote.count.biz.constant;

public class RedisKeyConstants {

    /**
     * Hash Field: 粉丝总数
     */
    public static final String FIELD_FANS_TOTAL = "fansTotal";

    /**
     * Hash Field: 关注总数
     */
    public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";

    /**
     * 用户维度计数 Key 前缀
     */
    private static final String COUNT_USER_KEY_PREFIX = "count:user:";

    /**
     * Hash Field: 笔记点赞总数
     */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";
    /**
     * 笔记维度计数 Key 前缀
     */
    private static final String COUNT_NOTE_KEY_PREFIX = "count:note:";

    /**
     * 构建用户维度计数 Key
     *
     * @param userId 用户ID
     * @return 用户维度计数 Key
     */
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

    /**
     * 构建笔记维度计数 Key
     *
     * @param noteId 笔记ID
     * @return 笔记维度计数 Key
     */
    public static String buildCountNoteKey(Long noteId) {
        return COUNT_NOTE_KEY_PREFIX + noteId;
    }


}