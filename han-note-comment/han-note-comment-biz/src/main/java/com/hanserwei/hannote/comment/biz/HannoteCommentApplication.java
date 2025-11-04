package com.hanserwei.hannote.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.comment.biz.domain.mapper")
public class HannoteCommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteCommentApplication.class, args);
    }
}
