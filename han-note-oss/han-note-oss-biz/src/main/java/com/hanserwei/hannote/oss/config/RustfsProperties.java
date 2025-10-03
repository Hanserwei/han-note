package com.hanserwei.hannote.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "storage.rustfs")
@Component
@Data
public class RustfsProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
}