package com.su.order.controller;

import com.su.order.bean.Order;
import com.su.order.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RefreshScope
@RequiredArgsConstructor
@RestController
@RequestMapping
public class OrderController {

    private final OrderService orderService;

    @Value("${order.timeout}")
    private String timeout;

    @Value("${order.auto-confirm}")
    private String autoConfirm;

    @Value("${order.jdbc.url}")
    private String url;

    //普通创建
    @RequestMapping("/create")
    public Order createOrder(@RequestParam Long productId, @RequestParam Long userId) {
        return orderService.createOrder(productId, userId);
    }

    //秒杀创建
    @RequestMapping("/seckill")
    public Order seckill(@RequestParam Long productId, @RequestParam Long userId) {
        Order order = orderService.createOrder(productId, userId);
        order.setId(Long.MAX_VALUE);
        return order;
    }

    @RequestMapping("/config")
    public String config() {
        return "timeout: " + timeout + ", autoConfirm: " + autoConfirm + ", url: " + url;
    }

    @GetMapping("/writeDb")
    public String writeBd() {
        return "writeBd";
    }

    @GetMapping("/readDb")
    public String readBd() {
        return "readBd";
    }

}