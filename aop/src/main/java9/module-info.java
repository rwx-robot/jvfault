/**
 * jvfault aop —— @Aspect 切面、execution pointcut、JDK/ByteBuddy 代理。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 * 字节码代理通过类加载器注入生成，需要直接访问 {@code java.lang} 内部类型，故使用
 * {@link #addReads} 提供必要的运行时反射可见性。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.aop {

    requires transitive com.jvfault.core;
    requires net.bytebuddy;
    requires org.slf4j;

    exports com.jvfault.aop;
    exports com.jvfault.aop.advice;
    exports com.jvfault.aop.annotation;
    exports com.jvfault.aop.aspect;
    exports com.jvfault.aop.interception;
    exports com.jvfault.aop.pointcut;
    exports com.jvfault.aop.processor;
    exports com.jvfault.aop.proxy;
}