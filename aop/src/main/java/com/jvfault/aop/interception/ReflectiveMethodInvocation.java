package com.jvfault.aop.interception;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 反射式方法调用 - 责任链执行器。
 * 顺序：拦截器列表依序进入，末端执行目标方法。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ReflectiveMethodInvocation implements MethodInvocation {

    private final Object target;
    private final Method method;
    private final Object[] args;
    private final List<MethodInterceptor> interceptors;
    private final Runnable[] onComplete;
    private int index = -1;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] args,
                                      List<MethodInterceptor> interceptors) {
        this(target, method, args, interceptors, new Runnable[0]);
    }

    public ReflectiveMethodInvocation(Object target, Method method, Object[] args,
                                      List<MethodInterceptor> interceptors, Runnable[] onComplete) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.interceptors = interceptors;
        this.onComplete = onComplete;
    }

    @Override
    public Object proceed() throws Throwable {
        if (index + 1 < interceptors.size()) {
            return interceptors.get(++index).invoke(this);
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        } finally {
            for (Runnable r : onComplete) {
                r.run();
            }
        }
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object getTarget() {
        return target;
    }
}
