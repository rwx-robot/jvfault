/**
 * jvfault mcp —— MCP 服务器。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.mcp {

    requires transitive com.jvfault.ai;
    requires transitive org.slf4j;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.mcp;
}