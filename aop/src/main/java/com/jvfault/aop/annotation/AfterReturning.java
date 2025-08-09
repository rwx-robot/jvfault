package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 返回通知，在目标方法正常返回后调用。通知方法第一个参数
 * （可选）注入返回值，参数类型须可承接实际返回类型。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterReturning {

    /** Pointcut 表达式 */
    String value();

    /** 绑定返回值的参数名（保留字段，参数按类型绑定） */
    String returning() default "";
}
