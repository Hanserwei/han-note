package com.hanserwei.hannote.search.service;

import org.springframework.http.ResponseEntity;

public interface ExtDictService {

    /**
     * 获取热更新词典
     *
     * @return 热更新词典
     */
    ResponseEntity<String> getHotUpdateExtDict();
}