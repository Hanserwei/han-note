package com.hanserwei.hannote.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.auth.domain.mapper")
public class HanNoteAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HanNoteAuthApplication.class, args);
    }

}
