package com.jvfault.aop.interception;

import java.lang.reflect.Method;

/**
 * 方法调用上下文（AOP Alliance MethodInvocation 风格）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface MethodInvocation {

    /** 继续调用链，返回目标方法（或后续拦截器）的结果 */
    Object proceed() throws Throwable;

    /** 目标方法 */
    Method getMethod();

    /** 方法实参 */
    Object[] getArguments();

    /** 目标对象（原始实例，非代理） */
    Object getTarget();
}
