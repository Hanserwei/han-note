package com.hanserwei.hannote.search.model.vo;

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
     * 头像
     */
    private String avatar;

    /**
     * 小憨书ID
     */
    @JsonProperty("han_note_id")
    private String hanNoteId;

    /**
     * 笔记发布总数
     */
    @JsonProperty("note_total")
    private Integer noteTotal;

    /**
     * 粉丝总数
     */
    @JsonProperty("fans_total")
    private Integer fansTotal;

}