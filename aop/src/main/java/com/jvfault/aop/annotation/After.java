package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 后置通知，在目标方法返回或抛出后都会调用（finally 语义）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface After {

    /** Pointcut 表达式 */
    String value();
}
