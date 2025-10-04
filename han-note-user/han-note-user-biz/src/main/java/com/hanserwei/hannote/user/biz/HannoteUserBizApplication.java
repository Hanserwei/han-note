package com.hanserwei.hannote.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.user.biz.domain.mapper")
public class HannoteUserBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteUserBizApplication.class, args);
    }
}
