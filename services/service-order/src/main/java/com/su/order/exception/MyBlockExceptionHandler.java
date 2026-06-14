package com.su.order.exception;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

@RequiredArgsConstructor
@Component
public class MyBlockExceptionHandler implements BlockExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       String resourceName, BlockException e) throws Exception {

        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        R<Object> error = R.error(500, resourceName + "被Sentinel限流，原因：" + e.getClass());
        String jsonError = objectMapper.writeValueAsString(error);
        writer.write(jsonError);

        writer.flush();
        writer.close();
    }
}