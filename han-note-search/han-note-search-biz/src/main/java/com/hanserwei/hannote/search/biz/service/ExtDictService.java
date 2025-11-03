package com.hanserwei.hannote.search.biz.service;

import org.springframework.http.ResponseEntity;

public interface ExtDictService {

    /**
     * 获取热更新词典
     *
     * @return 热更新词典
     */
    ResponseEntity<String> getHotUpdateExtDict();
}