package com.toonflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.toonflow.mapper")
public class ToonflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToonflowApplication.class, args);
    }
}
