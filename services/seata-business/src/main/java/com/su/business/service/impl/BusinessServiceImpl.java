package com.su.business.service.impl;

import com.su.business.feign.OrderFeignClient;
import com.su.business.feign.StorageFeignClient;
import com.su.business.service.BusinessService;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BusinessServiceImpl implements BusinessService {

    @Autowired
    private StorageFeignClient storageFeignClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Override
    @GlobalTransactional
    public void purchase(String userId, String commodityCode, int orderCount) {
        // 1. 扣减库存
        storageFeignClient.deduct(commodityCode, orderCount);
        // 2. 创建订单
        orderFeignClient.create(userId, commodityCode, orderCount);
    }
}
