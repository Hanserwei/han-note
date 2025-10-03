package com.hanserwei.hannote.oss.service;

import com.hanserwei.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传文件
     * 
     * @param file  文件
     * @return 文件上传结果
     */
    Response<?> uploadFile(MultipartFile file);
}