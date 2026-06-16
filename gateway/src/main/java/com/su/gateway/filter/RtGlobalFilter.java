package com.su.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

//全局过滤器
@Component
@Slf4j
public class RtGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        long startTime = System.currentTimeMillis();
        log.info("请求[{}]开始，时间：{}", request.getURI(), startTime);

        Mono<Void> filter = chain.filter(exchange);
        filter.doFinally((result) -> {
            long endTime = System.currentTimeMillis();
            log.info("请求[{}]结束，时间：{}，耗时：{}", exchange.getRequest().getURI(), endTime, endTime - startTime);
        });
        return filter;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
