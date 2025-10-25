/**
 * jvfault tracing —— W3C traceparent、Span/Tracer。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.tracing {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.tracing;
}