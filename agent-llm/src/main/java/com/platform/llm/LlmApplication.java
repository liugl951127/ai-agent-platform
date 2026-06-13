package com.platform.llm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.platform.llm","com.platform.common"})
@MapperScan("com.platform.llm.mapper")
public class LlmApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmApplication.class, args);
    }
}
