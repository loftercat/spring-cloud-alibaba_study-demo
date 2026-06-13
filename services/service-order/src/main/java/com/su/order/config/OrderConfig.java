package com.su.order.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Retryer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Configuration
public class OrderConfig {

    // 配置feign的请求拦截器，为每个请求添加X-Request-Id头
    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate request) -> request.header("X-Request-Id", UUID.randomUUID().toString());
    }
    
    // 配置feign的重试策略，默认5次
    @Bean
    public Retryer retryer() {
        return new Retryer.Default();
    }

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
