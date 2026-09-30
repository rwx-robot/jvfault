package com.jvfault.example.springbridge;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;

/**
 * jvfault 组件（正向：它会被暴露成 Spring bean，供 {@link DemoController} 注入；
 * 反向：它通过 {@code @Inject} 使用来自 Spring 容器的 {@link ClockService}）。
 *
 * <p>注意这里的 {@code @Component} 是 {@code com.jvfault.core.annotation.Component}，
 * 不是 Spring 的注解 —— Spring 的组件扫描不会碰它，jvfault 的扫描器负责实例化它。
 *
 * @since v1.0.14 (2026)
 */
@Component
public class Greeter {

    @Inject
    private ClockService clock;

    public String greet(String who) {
        return "hi " + who + ", jvfault x Spring bridged at " + clock.now();
    }
}
