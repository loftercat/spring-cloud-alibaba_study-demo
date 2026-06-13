package com.su.product.service;

import com.su.product.bean.Product;
import com.su.product.service.impl.ProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductServiceImpl implements ProductService {


    @Override
    public Product getProductById(Long productId) throws InterruptedException {
//        Thread.sleep(50000);
        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(100));
        product.setProductName("apple" + productId);
        product.setNum(2);
        return product;
    }
}