package com.su.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
public class DiscoveryTest {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    void discoveryClientTest() {
        // 获取所有注册的服务名称并打印
        discoveryClient.getServices().stream().map(s -> "service: " + s).forEach(System.out::println);
        
        // 获取指定服务的所有实例信息并打印主机地址
        discoveryClient.getInstances("service-order").stream().map(s -> "ip: " + s.getHost()).forEach(System.out::println);
    }
}
