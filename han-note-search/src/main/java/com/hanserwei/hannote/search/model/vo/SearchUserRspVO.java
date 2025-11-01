package com.hanserwei.hannote.search.model.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchUserRspVO {

    /**
     * 用户ID
     */
    @JsonProperty("id")
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 昵称：关键词高亮
     */
    private String highlightNickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 小憨书ID
     */
    @JsonAlias("han_note_id")
    private String hanNoteId;

    /**
     * 笔记发布总数
     */
    @JsonAlias("note_total")
    private Integer noteTotal;

    /**
     * 粉丝总数
     */
    @JsonAlias("fans_total")
    private String fansTotal;

}