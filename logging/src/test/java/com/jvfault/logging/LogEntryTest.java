package com.jvfault.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogEntry builder 测试 —— 默认值、MDC 读取与键值对顺序/不可变
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("LogEntry Tests")
class LogEntryTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Builder 默认应自动填充 timestamp 与 threadName")
    void testBuilderDefaults() {
        LogEntry entry = LogEntry.builder().message("x").build();

        assertTrue(entry.getTimestamp() > 0, "timestamp 应自动填充");
        assertEquals(Thread.currentThread().getName(), entry.getThreadName(), "threadName 应自动填充");
        assertEquals(LogLevel.INFO, entry.getLevel(), "默认级别为 INFO");
        assertEquals("x", entry.getMessage());
        assertTrue(entry.getKeyValuePairs().isEmpty());
        assertNull(entry.getTraceId());
        assertNull(entry.getSpanId());
    }

    @Test
    @DisplayName("显式设置的字段应覆盖默认值")
    void testExplicitOverrides() {
        LogEntry entry = LogEntry.builder()
                .timestamp(1700000000123L)
                .level(LogLevel.ERROR)
                .loggerName("com.demo.Order")
                .threadName("worker-1")
                .message("boom")
                .build();

        assertEquals(1700000000123L, entry.getTimestamp());
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("com.demo.Order", entry.getLoggerName());
        assertEquals("worker-1", entry.getThreadName());
        assertEquals("boom", entry.getMessage());
    }

    @Test
    @DisplayName("keyValuePairs 应保持插入顺序且构建后不可变")
    void testKeyValueOrderAndImmutability() {
        LogEntry entry = LogEntry.builder()
                .keyValue("b", 1)
                .keyValue("a", 2)
                .keyValue("c", 3)
                .build();

        Object[] keys = entry.getKeyValuePairs().keySet().toArray();
        assertEquals(3, keys.length);
        assertEquals("b", keys[0]);
        assertEquals("a", keys[1]);
        assertEquals("c", keys[2]);

        assertThrows(UnsupportedOperationException.class,
                () -> entry.getKeyValuePairs().put("x", "y"), "构建后键值对应为只读");
    }

    @Test
    @DisplayName("fromMdc 应从 MDC 读取 traceId/spanId")
    void testFromMdc() {
        MDC.put(LogEntry.MDC_TRACE_ID, "t-123");
        MDC.put(LogEntry.MDC_SPAN_ID, "s-456");
        try {
            LogEntry entry = LogEntry.builder().message("m").fromMdc().build();

            assertEquals("t-123", entry.getTraceId());
            assertEquals("s-456", entry.getSpanId());
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("显式设置的 traceId 优先于 MDC 值")
    void testFromMdcKeepsExplicitValue() {
        MDC.put(LogEntry.MDC_TRACE_ID, "from-mdc");
        MDC.put(LogEntry.MDC_SPAN_ID, "s-mdc");
        try {
            LogEntry entry = LogEntry.builder().message("m").traceId("explicit").fromMdc().build();

            assertEquals("explicit", entry.getTraceId(), "显式值应优先");
            assertEquals("s-mdc", entry.getSpanId(), "未显式设置的字段从 MDC 填充");
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("MDC 为空时 fromMdc 不产生追踪字段")
    void testFromMdcWhenEmpty() {
        LogEntry entry = LogEntry.builder().message("m").fromMdc().build();

        assertNull(entry.getTraceId());
        assertNull(entry.getSpanId());
    }

    @Test
    @DisplayName("keyValuePairs(Map) 应合并键值对并跳过非法键")
    void testMergeFromMap() {
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("k1", "v1");
        extra.put(null, "ignored");

        LogEntry entry = LogEntry.builder()
                .keyValue("k0", "v0")
                .keyValuePairs(extra)
                .build();

        assertEquals("v0", entry.getKeyValuePairs().get("k0"));
        assertEquals("v1", entry.getKeyValuePairs().get("k1"));
        assertFalse(entry.getKeyValuePairs().containsKey(null), "null 键应被忽略");
    }

    @Test
    @DisplayName("LinkedHashMap 类型的键值对应保持顺序")
    void testLinkedHashMapPreservesOrder() {
        Map<String, Object> ordered = new LinkedHashMap<String, Object>();
        ordered.put("second", 2);
        ordered.put("first", 1);

        LogEntry entry = LogEntry.builder().keyValuePairs(ordered).build();

        Object[] keys = entry.getKeyValuePairs().keySet().toArray();
        assertEquals("second", keys[0]);
        assertEquals("first", keys[1]);
    }
}
