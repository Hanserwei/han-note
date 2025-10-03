package com.hanserwei.hannote.oss.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class RustfsConfig {

    @Resource
    private RustfsProperties rustfsProperties;

    @Bean
    public S3Client minioClient() {
        // 构建 Rustfs 客户端
        return S3Client.builder()
                .endpointOverride(URI.create(rustfsProperties.getEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(() -> AwsBasicCredentials.create(rustfsProperties.getAccessKey(), rustfsProperties.getSecretKey()))
                .forcePathStyle(true)
                .build();
    }
}