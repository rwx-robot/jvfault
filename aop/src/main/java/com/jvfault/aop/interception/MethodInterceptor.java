package com.jvfault.aop.interception;

/**
 * 方法拦截器（责任链节点）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@FunctionalInterface
public interface MethodInterceptor {

    Object invoke(MethodInvocation invocation) throws Throwable;
}
