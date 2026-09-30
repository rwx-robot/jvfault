package com.jvfault.spring.boot.starter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 <b>Spring bean</b> 需要被<b>反向注入</b> jvfault 容器。
 *
 * <p>仅当 {@code jvfault.import-spring-beans=true} 时生效。被标注的 Spring 单例会被
 * 注册进 jvfault 的 {@code BeanRegistry}，于是 jvfault 的 {@code @Component} 可以通过
 * {@code @Inject} 直接拿到它 —— 这就是 “Spring → jvfault” 的反向桥接。
 *
 * <p>刻意要求<b>显式标注</b>：不把整个 Spring 容器倒灌进 jvfault，避免两个容器的
 * 生命周期与依赖解析纠缠在一起（循环依赖极难诊断）。只有你明确想给 jvfault 用的
 * bean 才打这个注解。
 *
 * <p>示例：
 * <pre>{@code
 * @Service
 * @JvfaultComponent
 * public class ClockService {
 *     public String now() { return Instant.now().toString(); }
 * }
 *
 * @Component                       // jvfault 组件
 * public class Greeter {
 *     @Inject ClockService clock; // 来自 Spring 容器
 *     public String greet(String who) { return "hi " + who + " @ " + clock.now(); }
 * }
 * }</pre>
 *
 * @since v1.0.14 (2026)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JvfaultComponent {
}
