package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 异常通知，在目标方法抛出后调用。通知方法第一个参数（可选，
 * 类型须为 Throwable 或其子类）注入抛出的异常。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterThrowing {

    /** Pointcut 表达式 */
    String value();

    /** 绑定异常的参数名（保留字段，参数按类型绑定） */
    String throwing() default "";
}
