package com.su.order.feign;

import com.su.order.config.OpenFeignLogConfig;
import com.su.product.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//远程调用，还可以调用其他的接口，不一定非要是nacos注册的服务 @FeignClient(name = "****service", url = "http://****/api/**")
@FeignClient(name = "service-product")
public interface ProductFeignClient {

    @GetMapping("/product/{productId}")
    Product getProductById(@PathVariable("productId") Long productId);
}