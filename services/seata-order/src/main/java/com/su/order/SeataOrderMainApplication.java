package com.su.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.su.order.mapper")
@EnableFeignClients(basePackages = "com.su.order.feign")
public class SeataOrderMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeataOrderMainApplication.class, args);
    }
}
