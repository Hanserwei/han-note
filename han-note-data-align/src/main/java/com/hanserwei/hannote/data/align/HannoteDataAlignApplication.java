package com.hanserwei.hannote.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.data.align.domain.mapper")
@EnableFeignClients(basePackages = "com.hanserwei.hannote")
public class HannoteDataAlignApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteDataAlignApplication.class, args);
    }
}
