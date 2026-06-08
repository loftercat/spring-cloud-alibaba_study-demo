package com.su.order.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * 订单参数自动刷新
 */
@Data
@Component
//@ConfigurationProperties(prefix = "order")
public class OrderArgsAutoRefresh {

    /*private String timeout;

    private String autoConfirm;

    private String url;*/

}