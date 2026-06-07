package com.su.order.controller;

import com.su.order.bean.Order;
import com.su.order.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @RequestMapping("/create")
    public Order createOrder(@RequestParam Long productId, @RequestParam Long userId) {
        return orderService.createOrder(productId, userId);
    }
}
