package com.hanserwei.hannote.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.count.biz.domain.mapper")
public class HannoteCountBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteCountBizApplication.class, args);
    }
}
