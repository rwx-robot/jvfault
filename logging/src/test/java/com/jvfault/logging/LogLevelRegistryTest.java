package com.jvfault.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogLevelRegistry 测试 —— 前缀覆盖、最长前缀匹配与恢复默认
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("LogLevelRegistry Tests")
class LogLevelRegistryTest {

    private final LogLevelRegistry registry = new LogLevelRegistry();

    @Test
    @DisplayName("无覆盖时应返回 null（交由底层 slf4j 决定）")
    void testNoOverrideReturnsNull() {
        assertNull(registry.getEffectiveLevel("com.jvfault.Order"));
        assertTrue(registry.getOverrides().isEmpty());
    }

    @Test
    @DisplayName("setLevel 应按前缀覆盖 StructuredLogger 的输出级别")
    void testSetLevelGatesStructuredLogger() {
        final List<LogEntry> captured = new ArrayList<LogEntry>();
        StructuredLogger logger = new StructuredLogger(
                LoggerFactory.getLogger("com.jvfault.test.Order"),
                new LogFormatter() {
                    @Override
                    public String format(LogEntry entry) {
                        captured.add(entry);
                        return "captured";
                    }
                },
                registry);

        registry.setLevel("com.jvfault", LogLevel.WARN);

        assertFalse(logger.isInfoEnabled(), "INFO 应被 WARN 覆盖拦截");
        logger.info("suppressed");
        assertTrue(captured.isEmpty(), "被覆盖拦截的日志不应进入格式化流程");

        assertTrue(logger.isWarnEnabled());
        logger.warn("kept");
        assertEquals(1, captured.size());
        assertEquals(LogLevel.WARN, captured.get(0).getLevel());

        registry.resetAll();
        assertTrue(logger.isInfoEnabled(), "resetAll 后应恢复默认");
        logger.info("restored");
        assertEquals(2, captured.size());
        assertEquals(LogLevel.INFO, captured.get(1).getLevel());
    }

    @Test
    @DisplayName("最长匹配前缀应优先")
    void testLongestPrefixWins() {
        registry.setLevel("com.foo", LogLevel.DEBUG);
        registry.setLevel("com.foo.bar", LogLevel.WARN);

        assertEquals(LogLevel.WARN, registry.getEffectiveLevel("com.foo.bar.Baz"), "更长的前缀应优先");
        assertEquals(LogLevel.DEBUG, registry.getEffectiveLevel("com.foo.Qux"));
        assertNull(registry.getEffectiveLevel("net.other"), "无匹配前缀应返回 null");
    }

    @Test
    @DisplayName("同一前缀重复设置应覆盖")
    void testOverwriteSamePrefix() {
        registry.setLevel("com.acme", LogLevel.DEBUG);
        registry.setLevel("com.acme", LogLevel.ERROR);

        assertEquals(LogLevel.ERROR, registry.getEffectiveLevel("com.acme.Service"));
        assertEquals(1, registry.getOverrides().size());
    }

    @Test
    @DisplayName("resetAll 应清空全部覆盖项")
    void testResetAll() {
        registry.setLevel("com.a", LogLevel.DEBUG);
        registry.setLevel("com.b", LogLevel.ERROR);
        assertEquals(2, registry.getOverrides().size());

        registry.resetAll();

        assertTrue(registry.getOverrides().isEmpty());
        assertNull(registry.getEffectiveLevel("com.a.Service"));
        assertNull(registry.getEffectiveLevel("com.b.Service"));
    }

    @Test
    @DisplayName("非法参数应抛出 IllegalArgumentException")
    void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> registry.setLevel(null, LogLevel.INFO));
        assertThrows(IllegalArgumentException.class, () -> registry.setLevel("", LogLevel.INFO));
        assertThrows(IllegalArgumentException.class, () -> registry.setLevel("com.x", null));
    }

    @Test
    @DisplayName("空 Logger 名称查询应返回 null")
    void testEmptyLoggerNameQuery() {
        registry.setLevel("com", LogLevel.DEBUG);

        assertNull(registry.getEffectiveLevel(null));
        assertNull(registry.getEffectiveLevel(""));
    }
}
