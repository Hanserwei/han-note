package com.hanserwei.hannote.distributed.id.generator.biz.core.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Result {
    private long id;
    private Status status;

    public Result() {

    }
    public Result(long id, Status status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
