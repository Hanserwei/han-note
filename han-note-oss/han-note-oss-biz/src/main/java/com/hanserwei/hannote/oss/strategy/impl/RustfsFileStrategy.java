package com.hanserwei.hannote.oss.strategy.impl;

import com.hanserwei.hannote.oss.config.RustfsProperties;
import com.hanserwei.hannote.oss.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
public class RustfsFileStrategy implements FileStrategy {

    @Resource
    private RustfsProperties rustfsProperties;

    @Resource
    private S3Client rustfsClient;

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("## 上传文件至 Rustfs ...");

        // 判断文件是否为空
        if (file == null || file.isEmpty()) {
            log.error("==> 上传文件异常：文件为空 ...");
            throw new RuntimeException("文件不能为空");
        }

        // 文件的原始名称
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 生成存储对象的名称
        String key = UUID.randomUUID().toString().replace("-", "");
        String suffix = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 拼接最终文件名
        String objectName = key + suffix;

        log.info("==> 开始上传文件至 Rustfs, ObjectName: {}", objectName);

        // 执行上传
        try (InputStream inputStream = file.getInputStream()) {
            rustfsClient.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName) // 方法参数传入 bucketName
                            .key(objectName)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        }

        // 返回文件的访问链接（注意：Rustfs 是否支持直链要看配置）
        String url = String.format("%s/%s/%s",
                rustfsProperties.getEndpoint().replaceAll("/$", ""), // 去掉结尾的斜杠
                bucketName,
                objectName);

        log.info("==> 上传文件至 Rustfs 成功，访问路径: {}", url);
        return url;
    }
}