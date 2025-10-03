package com.hanserwei.hannote.oss.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface FileStrategy {

    /**
     * 文件上传
     * 
     * @param file  文件
     * @param bucketName 存储桶名称
     * @return 文件访问路径
     */
    String uploadFile(MultipartFile file, String bucketName);

}