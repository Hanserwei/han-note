package com.hanserwei.hannote.distributed.id.generator.biz.config;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LeafDataSourceConfiguration {

    private final LeafProperties leafProperties;

    @PostConstruct
    void logDataSourceSource() {
        LeafProperties.Jdbc jdbc = leafProperties.getJdbc();
        if (StringUtils.hasText(jdbc.getUrl())) {
            log.info("Leaf JDBC properties detected, will configure DruidDataSource via leaf.jdbc.*");
        } else {
            log.info("Leaf JDBC properties not set, relying on default spring.datasource configuration");
        }
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(prefix = "leaf.jdbc", name = "url")
    public DataSource leafDataSource() {
        LeafProperties.Jdbc jdbc = leafProperties.getJdbc();
        var dataSource = DruidDataSourceBuilder.create().build();
        dataSource.setUrl(jdbc.getUrl());
        dataSource.setUsername(jdbc.getUsername());
        dataSource.setPassword(jdbc.getPassword());
        dataSource.setDriverClassName(jdbc.getDriverClassName());
        return dataSource;
    }
}
