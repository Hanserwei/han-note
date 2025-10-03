package com.hanserwei.hannote.oss.service.impl;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.oss.service.FileService;
import com.hanserwei.hannote.oss.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    @Resource
    private FileStrategy fileStrategy;

    private static final String BUCKET_NAME = "han-note";

    @Override
    public Response<?> uploadFile(MultipartFile file) {
        // 上传文件
        String url = fileStrategy.uploadFile(file, BUCKET_NAME);
        return Response.success(url);
    }
}
