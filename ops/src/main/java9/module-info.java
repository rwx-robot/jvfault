/**
 * jvfault ops —— HealthIndicator/HealthAggregator（liveness/readiness 探针）、
 * GracefulShutdown。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.ops {

    requires transitive com.jvfault.core;
    requires transitive com.jvfault.metrics;
    requires transitive com.jvfault.tracing;
    requires transitive org.slf4j;

    exports com.jvfault.ops;
}