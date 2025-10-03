package com.hanserwei.hannote.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "storage.cos")
public class CosProperties {
    private String endpoint;
    private String secretId;
    private String secretKey;
    private String appId;
    private String region;
}
