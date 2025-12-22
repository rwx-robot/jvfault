/**
 * jvfault logging —— 结构化 JSON 日志、MDC trace 透传、动态级别。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.logging {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.logging;
}
