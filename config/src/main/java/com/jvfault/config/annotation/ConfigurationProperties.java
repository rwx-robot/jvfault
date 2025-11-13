package com.jvfault.config.annotation;

import java.lang.annotation.*;

/**
 * 配置属性批量绑定（前缀下的键自动绑定到同名字段）。
 * 对应 Spring: @ConfigurationProperties
 *
 * <p>支持：String/基本类型及包装类、枚举、List&lt;String&gt;、
 * 嵌套 POJO 字段（递归绑定）。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

    /** 配置键前缀 */
    String value();
}
