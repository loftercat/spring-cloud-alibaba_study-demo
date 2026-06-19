package com.su.order.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.su.order.bean.Order;
import com.su.order.feign.ProductFeignClient;
import com.su.order.service.OrderService;
import com.su.product.bean.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate;

    private final LoadBalancerClient loadBalancerClient;

    private final ProductFeignClient productFeignClient;

    //如果对createOrder进行控流，出现控制后，先找handler，再找fallback，如果都没有则抛出异常，可以在全局异常处理器中捕获处理
    @SentinelResource(value = "createOrder", blockHandler = "createOrderHandler")
    @Override
    public Order createOrder(Long productId, Long userId) {
        //Product product = getProductFromRemoteWithBalanceAnnotation(productId);
        Product product = productFeignClient.getProductById(productId);
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(product.getPrice().multiply(new BigDecimal(product.getNum())));
        order.setUserId(userId);
        order.setNickname("az");
        order.setAddress("公寓");
        order.setProductList(List.of(product));
        return order;
    }

    //如果@SentinelResource(value = "createOrder")没有被控流，则正常返回数据，否则返回次方法兜底数据
    public Order createOrderHandler(Long productId, Long userId, BlockException e) {
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.valueOf(0));
        order.setUserId(userId);
        order.setNickname("handler兜底");
        order.setAddress("异常信息：" + e.getMessage());
        order.setProductList(List.of());
        return order;
    }

    /**
     * 从远程服务获取商品信息
     * @param productId 商品id
     * @return 商品信息
     */
    private Product getProductFromRemote(Long productId) {
        // 从远程服务获取商品信息
        List<ServiceInstance> instances = discoveryClient.getInstances("service-product");
        ServiceInstance instance = instances.get(0);
        String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/product/" + productId;
        log.info("远程请求url: {}", url);
        return restTemplate.getForObject(url, Product.class);
    }

    /**
     * 从远程服务获取商品信息,带负载均衡的请求
     * @param productId 商品id
     * @return 商品信息
     */
    private Product getProductFromRemoteWithBalance(Long productId) {
        // 从远程服务获取商品信息
        ServiceInstance instance = loadBalancerClient.choose("service-product");
        String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/product/" + productId;
        log.info("远程请求url: {}，服务实例: {}", url, instance.getHost() + ":" + instance.getPort());
        return restTemplate.getForObject(url, Product.class);
    }

    /**
     * 从远程服务获取商品信息,带负载均衡的请求,使用注解配置负载均衡策略
     * @param productId 商品id
     * @return 商品信息
     */
    private Product getProductFromRemoteWithBalanceAnnotation(Long productId) {
        // 从远程服务获取商品信息
        String url = "http://service-product/product/" + productId;
        log.info("远程请求url: {}", url);
        return restTemplate.getForObject(url, Product.class);
    }
}
