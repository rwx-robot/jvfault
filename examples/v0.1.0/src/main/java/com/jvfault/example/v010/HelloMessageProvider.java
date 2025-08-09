package com.jvfault.example.v010;

import com.jvfault.core.annotation.Component;

/**
 * 默认消息提供者实现
 */
@Component
public class HelloMessageProvider implements MessageProvider {

    @Override
    public String getMessage() {
        return "Hello";
    }
}