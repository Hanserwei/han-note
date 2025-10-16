package com.hanserwei.hannote.note.biz.constant;

public class RedisKeyConstants {

    /**
     * 笔记详情 KEY 前缀
     */
    public static final String NOTE_DETAIL_KEY = "note:detail:";

    /**
     * 布隆过滤器：用户笔记点赞
     */
    public static final String BLOOM_USER_NOTE_LIKE_LIST_KEY = "bloom:note:likes:";


    /**
     * 构建完整的笔记详情 KEY
     * @param noteId 笔记ID
     * @return 笔记详情 KEY
     */
    public static String buildNoteDetailKey(Long noteId) {
        return NOTE_DETAIL_KEY + noteId;
    }

    /**
     * 构建完整的布隆过滤器：用户笔记点赞 KEY
     *
     * @param userId 用户ID
     * @return 布隆过滤器：用户笔记点赞 KEY
     */
    public static String buildBloomUserNoteLikeListKey(Long userId) {
        return BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
    }

}