package com.jvfault.core.annotation;

import java.lang.annotation.*;

/**
 * 构造器/字段/方法注入点标记。
 * 对应 Spring: @Autowired
 *
 * <p>与 JSR-330 {@code jakarta.inject.Inject} 语义兼容，可任选其一使用；
 * 结合 {@code @Named} 消除同类型多实现的歧义。
 *
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
}
