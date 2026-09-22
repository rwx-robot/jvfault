package com.example.service;

/**
 * Pointcut 匹配测试夹具（FQN: com.example.service.OrderService）
 */
public class OrderService {

    public String saveOrder(String item) {
        return "saved:" + item;
    }

    public java.util.List<String> listAll() {
        return java.util.Collections.emptyList();
    }
}
