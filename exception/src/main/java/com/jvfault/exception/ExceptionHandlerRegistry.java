package com.jvfault.exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 异常处理器注册表 - 扫描处理器 Bean 上的 @ExceptionHandler 方法。
 *
 * <p>解析规则：继承距离最短（最具体）的处理器胜出；
 * 距离相同时先注册者优先。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ExceptionHandlerRegistry {

    /** exceptionType -> (distance, handler) */
    private final Map<Class<? extends Throwable>, List<Entry>> byType = new LinkedHashMap<>();
    private final List<Object> handlerBeans = new ArrayList<>();

    /**
     * 注册处理器 Bean，扫描其（含父类）@ExceptionHandler 方法。
     */
    public void register(Object handlerBean) {
        handlerBeans.add(handlerBean);
        Class<?> clazz = handlerBean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                ExceptionHandler ann = method.getAnnotation(ExceptionHandler.class);
                if (ann == null) {
                    continue;
                }
                method.setAccessible(true);
                for (Class<? extends Throwable> type : ann.value()) {
                    byType.computeIfAbsent(type, k -> new ArrayList<>())
                            .add(new Entry(handlerBean, method, 0)); // declaredDistance 永远为 0（自身类型）
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 解析最匹配的处理器（继承距离最短，同距离先注册者优先）。
     */
    public Optional<HandlerMethod> resolve(Throwable throwable) {
        Entry best = null;
        for (Map.Entry<Class<? extends Throwable>, List<Entry>> e : byType.entrySet()) {
            if (!e.getKey().isInstance(throwable)) {
                continue;
            }
            int d = inheritanceDistance(e.getKey(), throwable.getClass());
            for (Entry entry : e.getValue()) {
                if (best == null || d < best.effectiveDistance) {
                    best = new Entry(entry.bean, entry.method, d);
                }
            }
        }
        return best == null ? Optional.empty() : Optional.of(new HandlerMethod(best.bean, best.method));
    }

    /** declaredException 与 thrown 实际类型的继承距离 */
    private int inheritanceDistance(Class<? extends Throwable> declared, Class<? extends Throwable> thrown) {
        int d = 0;
        Class<?> c = thrown;
        while (c != null) {
            if (c == declared) {
                return d;
            }
            c = c.getSuperclass();
            d++;
        }
        return Integer.MAX_VALUE;
    }

    public Collection<Object> getHandlerBeans() {
        return Collections.unmodifiableList(handlerBeans);
    }

    private static class Entry {
        final Object bean;
        final Method method;
        final int effectiveDistance;

        Entry(Object bean, Method method, int effectiveDistance) {
            this.bean = bean;
            this.method = method;
            this.effectiveDistance = effectiveDistance;
        }
    }

    /**
     * 处理器方法引用。
     */
    public static class HandlerMethod {

        private final Object bean;
        private final Method method;

        HandlerMethod(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        public Object getBean() {
            return bean;
        }

        public Method getMethod() {
            return method;
        }

        /**
         * 调用处理器：参数按类型注入（Throwable、ProblemDetail 可选）。
         */
        public Object invoke(Throwable throwable, ProblemDetail fallback) throws Throwable {
            Class<?>[] pTypes = method.getParameterTypes();
            Object[] args = new Object[pTypes.length];
            for (int i = 0; i < pTypes.length; i++) {
                if (pTypes[i].isInstance(throwable)) {
                    args[i] = throwable;
                } else if (pTypes[i] == ProblemDetail.class) {
                    args[i] = fallback;
                }
            }
            try {
                return method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }
}
