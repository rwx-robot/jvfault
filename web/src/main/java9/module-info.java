/**
 * jvfault web —— @Controller 路由、Guard/拦截器管道、参数绑定。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.web {

    requires transitive com.jvfault.core;
    requires transitive com.jvfault.aop;
    requires transitive com.jvfault.exception;
    requires transitive com.jvfault.validation;
    requires transitive org.slf4j;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.web;
    exports com.jvfault.web.annotation;
    exports com.jvfault.web.exception;
    exports com.jvfault.web.http;
    exports com.jvfault.web.pipeline;
    exports com.jvfault.web.routing;
}