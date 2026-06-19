package com.su.order.service;

import com.su.order.bean.Order;

public interface OrderService {
    Order createOrder(Long productId, Long userId);
}
