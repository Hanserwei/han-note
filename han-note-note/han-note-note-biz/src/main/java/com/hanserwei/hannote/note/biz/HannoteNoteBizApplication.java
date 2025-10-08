package com.hanserwei.hannote.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.note.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.hanserwei.hannote")
public class HannoteNoteBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteNoteBizApplication.class, args);
    }
}
