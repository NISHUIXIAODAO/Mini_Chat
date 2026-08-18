package com.easychat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.easychat.mapper")
@EnableAsync
@EnableScheduling
public class EasyChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyChatApplication.class, args);
    }
}
