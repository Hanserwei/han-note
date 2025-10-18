package com.hanserwei.hannote.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectStatusEnum {
    COLLECT(1), // 收藏
    UNCOLLECTED(0), // 取消收藏
    ;

    private final Integer code;
}
