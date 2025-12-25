/**
 * jvfault platform-servlet —— Servlet 5.0 桥接（基于 web 模块）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.platform.servlet {

    requires transitive com.jvfault.web;
    requires transitive org.slf4j;

    exports com.jvfault.platform.servlet;
}