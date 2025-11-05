package com.hanserwei.hannote.kv.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentContentReqDTO {

    @NotNull(message = "笔记noteId不能为空")
    private Long noteId;

    @NotNull(message = "发布年月不能为空")
    private String yearMonth;

    @NotNull(message = "评论正文id不能为空")
    private String contentId;

    @NotNull(message = "评论正文内容不能为空")
    private String content;
}
