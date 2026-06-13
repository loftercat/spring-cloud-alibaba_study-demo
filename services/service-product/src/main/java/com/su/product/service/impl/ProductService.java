package com.su.product.service.impl;

import com.su.product.bean.Product;
import org.springframework.stereotype.Component;

public interface ProductService {
    /**
     * 根据商品id查询商品信息
     * @param productId 商品id
     * @return 商品信息
     */
    Product getProductById(Long productId) throws InterruptedException;
}
