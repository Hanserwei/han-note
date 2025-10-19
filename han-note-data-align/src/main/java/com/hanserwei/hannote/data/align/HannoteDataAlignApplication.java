package com.hanserwei.hannote.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.data.align.domain.mapper")
public class HannoteDataAlignApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteDataAlignApplication.class, args);
    }
}
