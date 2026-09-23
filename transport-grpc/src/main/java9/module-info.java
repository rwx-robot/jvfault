/**
 * jvfault transport-grpc —— gRPC 传输（grpc-netty 1.62）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.grpc {

    requires transitive com.jvfault.microservices;
    requires transitive org.slf4j;
    requires io.grpc;
    requires io.grpc.stub;
    requires io.grpc.netty;

    exports com.jvfault.transport.grpc;
}