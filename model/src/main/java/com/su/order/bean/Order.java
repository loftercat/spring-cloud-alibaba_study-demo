package com.su.order.bean;

import com.su.product.bean.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Order {
    private Long id;
    private BigDecimal totalPrice;
    private Long userId;
    private String nickname;
    private String address;
    private String productName;
    private List<Product> productList;
}
