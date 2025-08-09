package com.jvfault.test;

import java.lang.annotation.*;

/**
 * 测试模块配置注解
 * 用于指定测试使用的模块类和扫描包
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestModule {
    Class<?>[] value() default {};
    String[] basePackages() default {};
}