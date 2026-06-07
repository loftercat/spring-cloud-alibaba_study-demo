package com.su.product.controller;

import com.su.product.bean.Product;
import com.su.product.service.impl.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @RequestMapping("/{id}")
    public Product getProduct(@PathVariable("id") Long productId) {
        return productService.getProductById(productId);
    }
}
