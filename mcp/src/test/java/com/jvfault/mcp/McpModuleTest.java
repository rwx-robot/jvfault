package com.jvfault.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-mcp 核心测试
 *
 * @since v0.11.0 (2025)
 */
@DisplayName("MCP 模块测试")
class McpModuleTest {

    private McpServer server() {
        return new McpServer("jvfault-mcp", "1.0.0")
                .tool("echo", "回显文本", args -> "echo: " + args.get("text"))
                .resource("file:///README.md", "# jvfault");
    }

    @Test
    @DisplayName("initialize 握手")
    void testInitialize() {
        String response = server().handle(
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"initialize\",\"params\":{}}");
        assertTrue(response.contains("jvfault-mcp"));
        assertTrue(response.contains("protocolVersion"));
        assertTrue(response.contains("\"id\":\"1\""));
    }

    @Test
    @DisplayName("tools/list 列出工具")
    void testToolsList() {
        String response = server().handle(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        assertTrue(response.contains("\"name\":\"echo\""));
    }

    @Test
    @DisplayName("tools/call 调用工具")
    void testToolsCall() {
        String response = server().handle(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"hello\"}}}");
        assertTrue(response.contains("echo: hello"));
        assertFalse(response.contains("error"));
    }

    @Test
    @DisplayName("未知工具返回 JSON-RPC 错误")
    void testUnknownTool() {
        String response = server().handle(
                "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"missing\",\"arguments\":{}}}");
        assertTrue(response.contains("\"error\""));
        assertTrue(response.contains("-32603"));
    }

    @Test
    @DisplayName("resources 读写")
    void testResources() {
        McpServer s = server();
        String list = s.handle("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"resources/list\"}");
        assertTrue(list.contains("file:///README.md"));

        String read = s.handle(
                "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"resources/read\","
                        + "\"params\":{\"uri\":\"file:///README.md\"}}");
        assertTrue(read.contains("jvfault"));
    }

    @Test
    @DisplayName("通知（无 id）返回 null")
    void testNotification() {
        assertNull(server().handle(
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
    }

    @Test
    @DisplayName("不支持的方法返回错误")
    void testUnsupportedMethod() {
        String response = server().handle(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"no/such\"}");
        assertTrue(response.contains("\"error\""));
    }
}
