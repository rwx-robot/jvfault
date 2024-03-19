/**
 * jvfault ai —— ChatModel SPI、向量检索（MCP 服务器见 {@code com.jvfault.mcp}）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.ai {

    requires transitive com.jvfault.core;
    requires transitive com.jvfault.web;
    requires transitive org.slf4j;

    exports com.jvfault.ai;
}