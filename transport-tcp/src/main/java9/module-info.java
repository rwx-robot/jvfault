/**
 * jvfault transport-tcp —— JDK NIO TCP 传输（4 字节帧协议）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.tcp {

    requires transitive com.jvfault.microservices;

    exports com.jvfault.transport.tcp;
}