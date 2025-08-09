package com.jvfault.aop.advice;

import java.lang.reflect.Method;

/**
 * 通知方法包装器 - 把 @Around/@Before/@After 等注解方法
 * 适配成可执行的调用单元。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class AdviceMethod {

    private final Object aspectInstance;
    private final Method method;
    private final AdviceKind kind;
    private final String pointcut;

    public AdviceMethod(Object aspectInstance, Method method, AdviceKind kind, String pointcut) {
        this.aspectInstance = aspectInstance;
        this.method = method;
        this.kind = kind;
        this.pointcut = pointcut;
        method.setAccessible(true);
    }

    public Object getAspectInstance() {
        return aspectInstance;
    }

    public Method getMethod() {
        return method;
    }

    public AdviceKind getKind() {
        return kind;
    }

    public String getPointcut() {
        return pointcut;
    }

    public Object invoke(Object... args) throws Throwable {
        try {
            return method.invoke(aspectInstance, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    /** 通知类型 */
    public enum AdviceKind {
        AROUND, BEFORE, AFTER, AFTER_RETURNING, AFTER_THROWING
    }
}
