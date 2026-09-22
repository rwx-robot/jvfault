package com.jvfault.core.annotation;

import jakarta.inject.Named;
import java.lang.annotation.*;

/**
 * 在 @Configuration 类中定义 Bean 的方法级注解。
 * 对应 Spring: @Bean
 * 
 * <p>方法返回值将被注册为容器管理的 Bean。
 * 支持初始化/销毁回调、作用域、主备选等。
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Named
public @interface Bean {

    /**
     * Bean 名称，默认使用方法名
     */
    String value() default "";

    /**
     * Bean 作用域: singleton | prototype | request
     */
    String scope() default "";

    /**
     * 初始化方法名 (对应 @PostConstruct)
     * 方法签名: void initMethod()
     */
    String initMethod() default "";

    /**
     * 销毁方法名 (对应 @PreDestroy)
     * 方法签名: void destroyMethod()
     */
    String destroyMethod() default "(inferred)";

    /**
     * 是否为主备选
     */
    boolean primary() default false;

    /**
     * 自动装配模式
     */
    Autowire autowire() default Autowire.NO;

    enum Autowire {
        NO,          // 不自动装配
        BY_NAME,     // 按名称自动装配
        BY_TYPE      // 按类型自动装配
    }
}