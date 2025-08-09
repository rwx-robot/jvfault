package com.jvfault.test;

import java.lang.annotation.*;

/**
 * 测试类字段/参数自动注入注解
 * 配合 JvfaultTestExtension 使用
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    boolean required() default true;
}