package com.jvfault.spring.boot.starter.reversefixture;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;
import com.jvfault.spring.boot.starter.fixture.ClockService;

/**
 * jvfault 组件：通过 {@code @Inject} 拿到<b>来自 Spring 容器</b>的 {@link ClockService}。
 * 这只有在 {@code jvfault.import-spring-beans=true} 且 ClockService 标注了
 * {@code @JvfaultComponent} 时才解析得出来 —— 用来验证反向注入确实打通。
 */
@Component
public class TimeAwareGreeter {

    @Inject
    private ClockService clock;

    public String greet(String who) {
        return "hello " + who + " @ " + clock.now();
    }
}
