package com.hanserwei.hannote.distributed.id.generator.biz.core;


import com.hanserwei.hannote.distributed.id.generator.biz.core.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
