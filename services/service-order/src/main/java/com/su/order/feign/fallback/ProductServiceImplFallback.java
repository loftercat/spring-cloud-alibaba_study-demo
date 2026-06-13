package com.su.order.feign.fallback;

import com.su.order.feign.ProductFeignClient;
import com.su.product.bean.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class ProductServiceImplFallback implements ProductFeignClient {
    @Override
    public Product getProductById(Long productId) {
        log.info("getProductById接口的兜底策略触发");
        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(500));
        product.setProductName("兜底" + productId);
        product.setNum(1);
        return product;
    }
}
