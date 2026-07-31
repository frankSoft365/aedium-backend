package com.microsoft.aediumbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.microsoft.aediumbackend.mapper")
@EnableScheduling
public class AediumBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AediumBackendApplication.class, args);
    }

}
