package com.hanserwei.hannote.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.comment.biz.domain.mapper")
@EnableFeignClients("com.hanserwei.hannote")
@EnableRetry
public class HannoteCommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteCommentApplication.class, args);
    }
}
