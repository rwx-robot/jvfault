package com.jvfault.core.annotation;

import java.lang.annotation.*;

/**
 * 标记一个类为配置类，用于定义 @Bean 方法。
 * 对应 Spring: @Configuration
 * 
 * <p>配置类中的 @Bean 方法会被容器调用，返回值注册为 Bean。
 * 支持 @Import 导入其他配置类。
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component // 配置类本身也是组件
public @interface Configuration {

    /**
     * 代理模式: 默认 ENHANCED (CGLIB 代理，支持 @Bean 方法拦截)
     * 可选 LITE (无代理，@Bean 方法直接调用)
     */
    ProxyMode proxyMode() default ProxyMode.ENHANCED;

    enum ProxyMode {
        ENHANCED,
        LITE
    }
}