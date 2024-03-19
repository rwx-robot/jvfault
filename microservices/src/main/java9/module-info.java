/**
 * jvfault microservices —— Transport SPI、@MessageHandler、客户端代理工厂、JSON 编解码。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.microservices {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.microservices;
    exports com.jvfault.microservices.annotation;
}