package com.hanserwei.hannote.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.hanserwei.hannote.search.domain.mapper")
public class HannoteSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteSearchApplication.class, args);
    }
}
