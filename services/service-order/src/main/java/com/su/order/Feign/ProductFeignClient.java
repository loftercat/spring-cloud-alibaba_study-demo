package com.su.order.Feign;

import com.su.product.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-product")
public interface ProductFeignClient {

    @GetMapping("/product/{productId}")
    Product getProductById(@PathVariable("productId") Long productId);
}
