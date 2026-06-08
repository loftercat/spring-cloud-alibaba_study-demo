package com.su.order.controller;

import com.su.order.bean.Order;
import com.su.order.config.OrderArgsAutoRefresh;
import com.su.order.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RefreshScope
@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

//    private final OrderArgsAutoRefresh orderArgsAutoRefresh;

    @Value("${order.timeout}")
    private String timeout;

    @Value("${order.auto-confirm}")
    private String autoConfirm;

    @Value("${order.jdbc.url}")
    private String url;

    @RequestMapping("/create")
    public Order createOrder(@RequestParam Long productId, @RequestParam Long userId) {
        return orderService.createOrder(productId, userId);
    }

    @RequestMapping("/config")
    public String config() {
        return "timeout: " + timeout + ", autoConfirm: " + autoConfirm + ", url: " + url;
    }


}