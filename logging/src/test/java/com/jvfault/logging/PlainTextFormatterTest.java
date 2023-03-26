package com.jvfault.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlainTextFormatter 测试 —— 人类可读格式与空段落省略
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("PlainTextFormatter Tests")
class PlainTextFormatterTest {

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:?\\d{2}");

    @Test
    @DisplayName("应包含时间戳/级别/Logger/线程/消息各段")
    void testCoreSegments() {
        LogEntry entry = LogEntry.builder()
                .level(LogLevel.WARN)
                .loggerName("com.demo.OrderService")
                .message("disk almost full")
                .build();

        String out = new PlainTextFormatter().format(entry);

        assertTrue(TIMESTAMP_PATTERN.matcher(out).find(), "应包含毫秒级时间戳与偏移: " + out);
        assertTrue(out.contains("[WARN]"));
        assertTrue(out.contains("[com.demo.OrderService]"));
        assertTrue(out.contains("[" + Thread.currentThread().getName() + "]"));
        assertTrue(out.contains(" disk almost full "));
        assertFalse(out.contains("\n"), "输出必须是单行");
    }

    @Test
    @DisplayName("应输出键值对与追踪上下文段落")
    void testKvAndTraceSegments() {
        LogEntry entry = LogEntry.builder()
                .message("order placed")
                .keyValue("orderId", 42)
                .keyValue("city", "上海")
                .traceId("tid-1")
                .spanId("sid-1")
                .build();

        String out = new PlainTextFormatter().format(entry);

        assertTrue(out.contains("{orderId=42, city=上海}"), "键值对应按 k=v 渲染: " + out);
        assertTrue(out.contains("[traceId=tid-1, spanId=sid-1]"), "追踪上下文应渲染");
    }

    @Test
    @DisplayName("键值对与追踪上下文为空时省略对应段落")
    void testOmitsEmptySegments() {
        LogEntry entry = LogEntry.builder().message("plain").build();

        String out = new PlainTextFormatter().format(entry);

        assertFalse(out.contains("{"), "无键值对时不应输出大括号段落: " + out);
        assertFalse(out.contains("traceId="));
        assertFalse(out.contains("spanId="));
    }

    @Test
    @DisplayName("仅有 traceId 时也应正确渲染追踪段")
    void testTraceIdOnly() {
        LogEntry entry = LogEntry.builder().message("m").traceId("tid-only").build();

        String out = new PlainTextFormatter().format(entry);

        assertTrue(out.contains("[traceId=tid-only]"));
        assertFalse(out.contains("spanId="));
    }
}
