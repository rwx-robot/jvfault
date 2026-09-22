package com.jvfault.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonLogFormatter 测试 —— JSON 转义、键值对输出与排序
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("JsonLogFormatter Tests")
class JsonLogFormatterTest {

    @Test
    @DisplayName("应输出核心字段且保持单行")
    void testCoreFieldsAndSingleLine() {
        LogEntry entry = LogEntry.builder()
                .level(LogLevel.INFO)
                .loggerName("com.demo.OrderService")
                .message("hello")
                .build();

        String json = new JsonLogFormatter().format(entry);

        assertTrue(json.startsWith("{\"timestamp\":"), "应以 timestamp 字段开头: " + json);
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"logger\":\"com.demo.OrderService\""));
        assertTrue(json.contains("\"thread\":\"" + Thread.currentThread().getName() + "\""));
        assertTrue(json.contains("\"message\":\"hello\""));
        assertFalse(json.contains("\n"), "输出必须是单行");
        assertFalse(json.contains("\"traceId\""), "traceId 为空时应省略");
        assertFalse(json.contains("\"kv\""), "键值对为空时应省略 kv 字段");
    }

    @Test
    @DisplayName("字符串转义应覆盖引号/反斜杠/换行/回车/制表符与控制字符")
    void testStringEscaping() {
        String message = "he said \"hi\"\\path\nnext\tend\u0001\r!";
        LogEntry entry = LogEntry.builder().message(message).build();

        String json = new JsonLogFormatter().format(entry);

        assertTrue(json.contains("\"message\":\"he said \\\"hi\\\"\\\\path\\nnext\\tend\\u0001\\r!\""),
                "消息应被正确转义: " + json);
        assertFalse(json.contains("\n"), "换行必须转义为 \\n");
        assertFalse(json.contains("\r"), "回车必须转义为 \\r");
        assertFalse(json.contains("\t"), "制表符必须转义为 \\t");
        assertFalse(json.contains("\u0001"), "控制字符必须转义");
    }

    @Test
    @DisplayName("键值对应按值类型正确渲染")
    void testKeyValueOutput() {
        LogEntry entry = LogEntry.builder()
                .message("kv test")
                .keyValue("city", "上海")
                .keyValue("count", 42)
                .keyValue("ratio", 1.5)
                .keyValue("flag", true)
                .keyValue("empty", null)
                .keyValue("obj", new Object() {
                    @Override
                    public String toString() {
                        return "Point(1,2)";
                    }
                })
                .build();

        String json = new JsonLogFormatter().format(entry);

        assertTrue(json.contains("\"kv\":{"), "存在键值对时应包含 kv 对象");
        assertTrue(json.contains("\"city\":\"上海\""), "字符串值应带引号: " + json);
        assertTrue(json.contains("\"count\":42"), "整数值应为字面量");
        assertTrue(json.contains("\"ratio\":1.5"), "小数值应为字面量");
        assertTrue(json.contains("\"flag\":true"), "布尔值应为字面量");
        assertTrue(json.contains("\"empty\":null"), "null 值应渲染为 null");
        assertTrue(json.contains("\"obj\":\"Point(1,2)\""), "其他对象应转为字符串");
    }

    @Test
    @DisplayName("sortKeys 选项应按键字典序排序，默认保持插入顺序")
    void testKeySortingOption() {
        LogEntry entry = LogEntry.builder()
                .message("sorted")
                .keyValue("zz", 1)
                .keyValue("aa", 2)
                .keyValue("mm", 3)
                .build();

        String sorted = new JsonLogFormatter(true).format(entry);
        assertTrue(sorted.indexOf("\"aa\"") < sorted.indexOf("\"mm\""), "排序后 aa 在 mm 之前: " + sorted);
        assertTrue(sorted.indexOf("\"mm\"") < sorted.indexOf("\"zz\""), "排序后 mm 在 zz 之前");
        assertTrue(new JsonLogFormatter(true).isSortKeys());

        String insertion = new JsonLogFormatter().format(entry);
        assertTrue(insertion.indexOf("\"zz\"") < insertion.indexOf("\"aa\""), "默认保持插入顺序: " + insertion);
        assertFalse(new JsonLogFormatter().isSortKeys());
    }

    @Test
    @DisplayName("traceId/spanId 存在时应输出到 JSON")
    void testTraceFields() {
        LogEntry entry = LogEntry.builder()
                .message("m")
                .traceId("tid-123")
                .spanId("sid-456")
                .build();

        String json = new JsonLogFormatter().format(entry);

        assertTrue(json.contains("\"traceId\":\"tid-123\""));
        assertTrue(json.contains("\"spanId\":\"sid-456\""));
    }

    @Test
    @DisplayName("中文字符不应被转义")
    void testUnicodePreserved() {
        LogEntry entry = LogEntry.builder().message("订单已创建：订单号 42").build();

        String json = new JsonLogFormatter().format(entry);

        assertTrue(json.contains("\"message\":\"订单已创建：订单号 42\""), "中文应原样保留: " + json);
    }

    @Test
    @DisplayName("null 消息应渲染为 JSON null 而非抛异常")
    void testNullMessage() {
        LogEntry entry = LogEntry.builder().message(null).build();

        String json = assertDoesNotThrow(() -> new JsonLogFormatter().format(entry));

        assertTrue(json.contains("\"message\":null"));
    }
}
