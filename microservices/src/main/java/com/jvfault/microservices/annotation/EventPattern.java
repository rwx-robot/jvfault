package com.jvfault.microservices.annotation;

import java.lang.annotation.*;

/**
 * 事件处理器（无响应）。方法首参数类型 = payload 类型。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventPattern {

    String value();
}
