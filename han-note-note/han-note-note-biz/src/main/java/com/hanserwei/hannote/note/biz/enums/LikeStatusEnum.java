package com.hanserwei.hannote.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeStatusEnum {
    LIKE(1), // 点赞
    DISLIKE(0), // 取消点赞
    ;

    private final Integer code;

}
