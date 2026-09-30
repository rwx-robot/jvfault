package com.jvfault.spring.boot.starter.collidefixture;

import com.jvfault.core.annotation.Component;

/**
 * jvfault 自有组件，jvfault 侧 bean 名为 {@code greeter}（简单名首字母小写）。
 *
 * <p>用它复现「Spring bean 名 == jvfault @Component 名」的撞名场景：
 * 测试里再往 Spring 注册一个同名的 {@code greeter} bean。
 */
@Component
public class Greeter {

    public String greet() {
        return "hi from jvfault";
    }
}
