/**
 * jvfault transport-redis —— Redis Pub/Sub 传输（Lettuce）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.redis {

    requires transitive com.jvfault.microservices;
    requires transitive org.slf4j;
    requires lettuce.core;

    exports com.jvfault.transport.redis;
}