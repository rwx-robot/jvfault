package com.jvfault.aop.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * ByteBuddy 子类代理工厂。
 *
 * <p>要求目标类为非 final 且拥有可访问构造器（沿用其构造器实例化）；
 * final/private/static 方法与 hashCode/equals/toString 不拦截。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
final class ByteBuddyClassProxy {

    private ByteBuddyClassProxy() {
    }

    /** 可抛异常的执行器（业务方法可能声明受检异常） */
    interface ThrowingExecutor {
        Object apply(Method method, Object[] args) throws Throwable;
    }

    /**
     * 创建子类代理。
     *
     * @param target 目标实例
     * @param executor (方法, 实参) -> 执行结果；由 ProxyFactory 提供统一链执行
     */
    @SuppressWarnings("unchecked")
    static <T> T create(T target, ThrowingExecutor executor) {
        try {
            DispatchHandler handler = new DispatchHandler(executor);
            Class<?> proxyClass = new ByteBuddy()
                    .subclass(target.getClass())
                    .method(ElementMatchers.not(ElementMatchers.isFinal())
                            .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                            .and(ElementMatchers.not(ElementMatchers.isStatic()))
                            .and(ElementMatchers.not(ElementMatchers.isHashCode()))
                            .and(ElementMatchers.not(ElementMatchers.isEquals()))
                            .and(ElementMatchers.not(ElementMatchers.isToString())))
                    .intercept(MethodDelegation.to(handler))
                    .make()
                    .load(target.getClass().getClassLoader(),
                            net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
            return (T) proxyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("ByteBuddy 类代理创建失败: "
                    + target.getClass().getName()
                    + " (类需为非 final 且有无参/可访问构造器)", e);
        }
    }

    /**
     * 方法分发器：全部被拦截方法交给 ProxyFactory 的执行器。
     */
    public static class DispatchHandler {

        private final ThrowingExecutor executor;

        DispatchHandler(ThrowingExecutor executor) {
            this.executor = executor;
        }

        @RuntimeType
        public Object dispatch(@Origin Method method, @AllArguments Object[] args) throws Throwable {
            return executor.apply(method, args);
        }
    }
}
