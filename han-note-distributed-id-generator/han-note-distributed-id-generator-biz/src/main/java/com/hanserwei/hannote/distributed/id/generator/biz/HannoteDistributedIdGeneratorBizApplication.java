package com.hanserwei.hannote.distributed.id.generator.biz;

import com.hanserwei.hannote.distributed.id.generator.biz.config.LeafProperties;
import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao.IDAllocMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LeafProperties.class)
@MapperScan(basePackageClasses = IDAllocMapper.class)
public class HannoteDistributedIdGeneratorBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteDistributedIdGeneratorBizApplication.class, args);
    }
}
