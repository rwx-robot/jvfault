/**
 * jvfault websocket —— @WebSocketGateway 网关与房间广播。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.websocket {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.websocket;
    exports com.jvfault.websocket.annotation;
    exports com.jvfault.websocket.net;
}