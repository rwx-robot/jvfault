package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 环绕通知。通知方法签名约定：
 * <pre>Object around(com.jvfault.aop.interception.MethodInvocation invocation) throws Throwable</pre>
 * 必须调用 {@code invocation.proceed()} 并返回其结果，否则目标方法不执行。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Around {

    /** Pointcut 表达式，如 execution(* com.example..*Service.save*(..)) */
    String value();
}
