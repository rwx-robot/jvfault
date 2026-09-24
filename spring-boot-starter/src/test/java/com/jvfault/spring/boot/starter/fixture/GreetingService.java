package com.jvfault.spring.boot.starter.fixture;

import com.jvfault.core.annotation.Component;

/** 测试用组件：由 jvfault 容器实例化，再被暴露成 Spring bean。 */
@Component
public class GreetingService {

    public String greet() {
        return "hello-jvfault";
    }
}
