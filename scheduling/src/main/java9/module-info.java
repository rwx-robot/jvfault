/**
 * jvfault scheduling —— @Scheduled + 6 字段 Cron。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.scheduling {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.scheduling;
    exports com.jvfault.scheduling.annotation;
    exports com.jvfault.scheduling.trigger;
}