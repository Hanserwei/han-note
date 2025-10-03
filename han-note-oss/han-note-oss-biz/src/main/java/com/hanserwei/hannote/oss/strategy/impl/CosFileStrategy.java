package com.hanserwei.hannote.oss.strategy.impl;

import com.hanserwei.hannote.oss.config.CosProperties;
import com.hanserwei.hannote.oss.strategy.FileStrategy;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
@Slf4j
public class CosFileStrategy implements FileStrategy {
    @Resource
    private CosProperties cosProperties;
    @Resource
    private COSClient cosClient;

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("## 上传文件至腾讯云Cos ...");

        // 判断文件是否为空
        if (file == null || file.getSize() == 0) {
            log.error("==> 上传文件异常：文件大小为空 ...");
            throw new RuntimeException("文件大小不能为空");
        }

        // 文件的原始名称
        String originalFileName = file.getOriginalFilename();

        // 生成存储对象的名称（将 UUID 字符串中的 - 替换成空字符串）
        String key = UUID.randomUUID().toString().replace("-", "");
        // 获取文件的后缀，如 .jpg
        String suffix = null;
        if (originalFileName != null) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 拼接上文件后缀，即为要存储的文件名
        String objectName = String.format("%s%s", key, suffix);

        log.info("==> 开始上传文件至腾讯云Cos, ObjectName: {}", objectName);

        // 设置元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // 执行上传
        try (InputStream inputStream = file.getInputStream()) {
            cosClient.putObject(bucketName, objectName, inputStream, metadata);
        }

        // 返回文件的访问链接
        String url = String.format("https://%s/%s", cosProperties.getEndpoint(), objectName);
        log.info("==> 上传文件至腾讯云 Cos 成功，访问路径: {}", url);
        return url;
    }
}
