/**
 * jvfault sse —— 规范事件流 SseEmitter / SseHub。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.sse {

    requires transitive com.jvfault.web;
    requires transitive org.slf4j;

    exports com.jvfault.sse;
}