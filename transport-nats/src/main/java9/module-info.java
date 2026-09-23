/**
 * jvfault transport-nats —— NATS 传输（jnats 2.17）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.nats {

    requires transitive com.jvfault.microservices;
    requires io.nats.jnats;

    exports com.jvfault.transport.nats;
}