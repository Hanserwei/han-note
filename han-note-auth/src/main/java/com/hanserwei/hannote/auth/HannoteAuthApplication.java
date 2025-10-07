package com.hanserwei.hannote.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.hanserwei.hannote")
public class HannoteAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteAuthApplication.class, args);
    }

}
