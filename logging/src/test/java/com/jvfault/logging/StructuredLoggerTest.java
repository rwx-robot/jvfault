package com.jvfault.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StructuredLogger 测试 —— 级别映射、键值对透传与 MDC 传播
 *
 * <p>使用捕获式 Formatter 观察进入格式化流程的 LogEntry；
 * 级别透传判断依赖 slf4j-simple（默认 INFO 级别）。
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("StructuredLogger Tests")
class StructuredLoggerTest {

    /** 捕获式格式化器：记录进入格式化流程的 LogEntry */
    private static final class CapturingFormatter implements LogFormatter {
        private final List<LogEntry> entries = new ArrayList<LogEntry>();

        @Override
        public String format(LogEntry entry) {
            entries.add(entry);
            return "captured|" + entry.getLevel() + "|" + entry.getMessage();
        }
    }

    private final CapturingFormatter formatter = new CapturingFormatter();
    private final StructuredLogger logger =
            new StructuredLogger(LoggerFactory.getLogger("com.jvfault.logging.test.Order"), formatter, null);

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("info 应构建含级别/Logger 名/消息/键值对的 LogEntry")
    void testInfoRendersStructuredEntry() {
        logger.info("订单已创建", KeyValuePair.of("orderId", 42), KeyValuePair.of("city", "上海"));

        assertEquals(1, formatter.entries.size(), "info 应进入格式化流程");
        LogEntry entry = formatter.entries.get(0);
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("com.jvfault.logging.test.Order", entry.getLoggerName());
        assertEquals("订单已创建", entry.getMessage());
        assertEquals(2, entry.getKeyValuePairs().size());
        assertEquals(Integer.valueOf(42), entry.getKeyValuePairs().get("orderId"));
        assertEquals("上海", entry.getKeyValuePairs().get("city"));
        assertEquals(Thread.currentThread().getName(), entry.getThreadName());
    }

    @Test
    @DisplayName("warn/error 应映射到对应级别")
    void testWarnErrorLevelMapping() {
        logger.warn("w", KeyValuePair.of("k", 1));
        logger.error("e");

        assertEquals(2, formatter.entries.size());
        assertEquals(LogLevel.WARN, formatter.entries.get(0).getLevel());
        assertEquals(LogLevel.ERROR, formatter.entries.get(1).getLevel());
        assertEquals(Integer.valueOf(1), formatter.entries.get(0).getKeyValuePairs().get("k"));
    }

    @Test
    @DisplayName("debug 受底层 slf4j 级别判断透传拦截（slf4j-simple 默认 INFO）")
    void testDebugGatedBySlf4jProvider() {
        assertFalse(logger.isDebugEnabled(), "slf4j-simple 默认 INFO，debug 应不可用");
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());

        logger.debug("should be dropped");

        assertTrue(formatter.entries.isEmpty(), "被底层拦截的 debug 不应进入格式化流程");
    }

    @Test
    @DisplayName("无键值对时 LogEntry 的键值对应为空")
    void testNoKeyValues() {
        logger.info("simple message");

        assertEquals(1, formatter.entries.size());
        assertTrue(formatter.entries.get(0).getKeyValuePairs().isEmpty());
    }

    @Test
    @DisplayName("MDC 中的 traceId/spanId 应传播到 LogEntry")
    void testMdcPropagation() {
        MDC.put(LogEntry.MDC_TRACE_ID, "trace-abc");
        MDC.put(LogEntry.MDC_SPAN_ID, "span-def");
        try {
            logger.info("with mdc");

            LogEntry entry = formatter.entries.get(0);
            assertEquals("trace-abc", entry.getTraceId());
            assertEquals("span-def", entry.getSpanId());
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("MDC 缺失时追踪字段为 null")
    void testMdcAbsent() {
        logger.info("no mdc");

        assertNull(formatter.entries.get(0).getTraceId());
        assertNull(formatter.entries.get(0).getSpanId());
    }

    @Test
    @DisplayName("KeyValuePair.of 空键应抛出 IllegalArgumentException")
    void testInvalidKeyValuePairKey() {
        assertThrows(IllegalArgumentException.class, () -> KeyValuePair.of(null, 1));
        assertThrows(IllegalArgumentException.class, () -> KeyValuePair.of("", 1));
    }
}
