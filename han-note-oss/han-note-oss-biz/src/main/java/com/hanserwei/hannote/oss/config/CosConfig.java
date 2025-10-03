package com.hanserwei.hannote.oss.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.endpoint.EndpointBuilder;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosConfig {

    @Resource
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient() {
        // 1. 初始化用户身份信息（SecretId, SecretKey）
        COSCredentials cred = new BasicCOSCredentials(
                cosProperties.getSecretId(),
                cosProperties.getSecretKey()
        );

        // 2. 设置 bucket 的地域
        Region region = new Region(cosProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        if (cosProperties.getEndpoint() != null && !cosProperties.getEndpoint().isEmpty()) {
            clientConfig.setEndpointBuilder(new EndpointBuilder() {
                @Override
                public String buildGeneralApiEndpoint(String bucketName) {
                    // 所有 API 请求都会使用自定义域名
                    return cosProperties.getEndpoint();
                }

                @Override
                public String buildGetServiceApiEndpoint() {
                    return cosProperties.getEndpoint();
                }
            });
        }

        // 3. 构建 COSClient
        return new COSClient(cred, clientConfig);
    }
}
