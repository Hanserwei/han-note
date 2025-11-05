package com.hanserwei.hannote.kv.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchAddCommentContentReqDTO {

    @Valid
    @NotEmpty(message = "评论内容集合不能为空")
    private List<CommentContentReqDTO> comments;
}
