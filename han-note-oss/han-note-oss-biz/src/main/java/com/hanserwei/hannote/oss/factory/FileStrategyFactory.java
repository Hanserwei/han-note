package com.hanserwei.hannote.oss.factory;

import com.hanserwei.hannote.oss.strategy.FileStrategy;
import com.hanserwei.hannote.oss.strategy.impl.AliyunOSSFileStrategy;
import com.hanserwei.hannote.oss.strategy.impl.CosFileStrategy;
import com.hanserwei.hannote.oss.strategy.impl.RustfsFileStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class FileStrategyFactory {

    @Value("${storage.type}")
    private String strategyType;

    @Bean
    @RefreshScope
    public FileStrategy getFileStrategy() {
        if (strategyType == null) {
            throw new IllegalArgumentException("存储类型不能为空");
        }

        return switch (strategyType.toLowerCase()) {
            case "rustfs" -> new RustfsFileStrategy();
            case "aliyun" -> new AliyunOSSFileStrategy();
            case "cos"    -> new CosFileStrategy();
            default -> throw new IllegalArgumentException("不可用的存储类型: " + strategyType);
        };
    }

}