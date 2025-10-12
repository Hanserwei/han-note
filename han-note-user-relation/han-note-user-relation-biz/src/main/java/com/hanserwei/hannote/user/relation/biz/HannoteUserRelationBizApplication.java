package com.hanserwei.hannote.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanserwei.hannote.user.relation.biz.domain.mapper")
public class HannoteUserRelationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(HannoteUserRelationBizApplication.class, args);
    }
}
