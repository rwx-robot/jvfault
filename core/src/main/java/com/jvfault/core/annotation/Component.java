package com.jvfault.core.annotation;

import jakarta.inject.Named;
import java.lang.annotation.*;

/**
 * 标记一个类为组件，由 IoC 容器管理生命周期。
 * 对应 Spring: @Component
 * 
 * <p>使用 JSR-330 @Named 指定 Bean 名称，支持别名。
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Named
public @interface Component {

    /**
     * Bean 名称 (别名)，默认由类名推导 (首字母小写)
     * 对应 Spring: value() / @Component("customName")
     */
    String value() default "";

    /**
     * 作用域: singleton (默认) | prototype | request
     */
    Scope scope() default Scope.SINGLETON;

    /**
     * 是否为主备选 (Primary)，用于自动装配时有多个同类型 Bean
     * 对应 Spring: @Primary
     */
    boolean primary() default false;

    /**
     * 作用域枚举
     */
    enum Scope {
        /** 单例模式 - 容器中只有一个实例 */
        SINGLETON,
        /** 原型模式 - 每次注入创建新实例 */
        PROTOTYPE,
        /** 请求作用域 - 每个 HTTP 请求一个实例 (Web 环境) */
        REQUEST
    }
}