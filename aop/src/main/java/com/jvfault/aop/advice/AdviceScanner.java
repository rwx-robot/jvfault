package com.jvfault.aop.advice;

import com.jvfault.aop.annotation.After;
import com.jvfault.aop.annotation.AfterReturning;
import com.jvfault.aop.annotation.AfterThrowing;
import com.jvfault.aop.annotation.Around;
import com.jvfault.aop.annotation.Before;
import com.jvfault.aop.interception.MethodInvocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知方法扫描器 - 从 @Aspect Bean 中提取全部通知方法。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public final class AdviceScanner {

    private AdviceScanner() {
    }

    /**
     * 扫描切面实例（本类与父类声明的方法）中的全部通知注解。
     */
    public static List<AdviceMethod> scan(Object aspectInstance) {
        List<AdviceMethod> result = new ArrayList<>();
        Class<?> clazz = aspectInstance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                AdviceMethod advice = toAdvice(aspectInstance, method);
                if (advice != null) {
                    result.add(advice);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    private static AdviceMethod toAdvice(Object aspectInstance, Method method) {
        Around around = method.getAnnotation(Around.class);
        if (around != null) {
            validateAroundSignature(method);
            return new AdviceMethod(aspectInstance, method, AdviceMethod.AdviceKind.AROUND, around.value());
        }
        Before before = method.getAnnotation(Before.class);
        if (before != null) {
            return new AdviceMethod(aspectInstance, method, AdviceMethod.AdviceKind.BEFORE, before.value());
        }
        After after = method.getAnnotation(After.class);
        if (after != null) {
            return new AdviceMethod(aspectInstance, method, AdviceMethod.AdviceKind.AFTER, after.value());
        }
        AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
        if (afterReturning != null) {
            return new AdviceMethod(aspectInstance, method,
                    AdviceMethod.AdviceKind.AFTER_RETURNING, afterReturning.value());
        }
        AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);
        if (afterThrowing != null) {
            return new AdviceMethod(aspectInstance, method,
                    AdviceMethod.AdviceKind.AFTER_THROWING, afterThrowing.value());
        }
        return null;
    }

    private static void validateAroundSignature(Method method) {
        boolean ok = method.getParameterCount() == 1
                && method.getParameterTypes()[0] == MethodInvocation.class;
        if (!ok) {
            throw new IllegalArgumentException("@Around 方法签名必须为 around(MethodInvocation): "
                    + method.getDeclaringClass().getName() + "." + method.getName());
        }
    }
}
