package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 前置通知，在目标方法执行前调用。通知方法参数可注入
 * MethodInvocation 或按类型注入目标方法实参。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Before {

    /** Pointcut 表达式 */
    String value();
}
