package com.su.order.controller;


import com.su.order.bean.OrderTbl;
import com.su.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderRestController {

    @Autowired
    OrderService orderService;

    /**
     * 创建订单
     */
    @GetMapping("/create")
    public String create(@RequestParam("userId") String userId,
                         @RequestParam("commodityCode") String commodityCode,
                         @RequestParam("count") int orderCount) {
        OrderTbl tbl = orderService.create(userId, commodityCode, orderCount);
        return "order create success = 订单id：[" + tbl.getId() + "]";
    }

}
