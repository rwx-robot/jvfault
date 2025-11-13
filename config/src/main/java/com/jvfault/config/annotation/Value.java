package com.jvfault.config.annotation;

import java.lang.annotation.*;

/**
 * 配置项注入。支持裸 key 与 ${key:default} 占位符形式。
 * 对应 Spring: @Value
 *
 * <pre>{@code
 * @Value("app.name")
 * private String appName;
 *
 * @Value("${app.timeout:30}")
 * private int timeout;
 * }</pre>
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

    /** 配置键或 ${key:default} 占位符表达式 */
    String value();
}
