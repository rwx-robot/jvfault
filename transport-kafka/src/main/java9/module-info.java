/**
 * jvfault transport-kafka —— Apache Kafka 传输（kafka-clients）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.kafka {

    requires transitive com.jvfault.microservices;
    requires transitive org.slf4j;
    requires kafka.clients;

    exports com.jvfault.transport.kafka;
}