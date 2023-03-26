package com.jvfault.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StructuredLoggerFactory 测试 —— 缓存语义与 formatter/registry 配置
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("StructuredLoggerFactory Tests")
class StructuredLoggerFactoryTest {

    /** 测试用服务类型 */
    static class SampleService {
    }

    @Test
    @DisplayName("同名 Logger 应返回同一实例")
    void testLoggerCaching() {
        StructuredLoggerFactory factory = new StructuredLoggerFactory();

        StructuredLogger a1 = factory.getLogger("com.demo.A");
        StructuredLogger a2 = factory.getLogger("com.demo.A");
        assertSame(a1, a2, "同名 Logger 应缓存复用");

        StructuredLogger byClass = factory.getLogger(SampleService.class);
        assertEquals(SampleService.class.getName(), byClass.getName(), "getLogger(Class) 应使用全限定名");
        assertSame(byClass, factory.getLogger(SampleService.class.getName()));
    }

    @Test
    @DisplayName("默认应使用 JsonLogFormatter 与独立级别注册表")
    void testDefaults() {
        StructuredLoggerFactory factory = new StructuredLoggerFactory();

        assertTrue(factory.getFormatter() instanceof JsonLogFormatter, "默认 formatter 应为 JsonLogFormatter");
        assertNotNull(factory.getLevelRegistry());
        assertSame(factory.getLevelRegistry(), factory.getLevelRegistry(), "级别注册表应全局唯一");
    }

    @Test
    @DisplayName("自定义 formatter 与 registry 应生效并传播到 Logger")
    void testCustomFormatterAndRegistry() {
        PlainTextFormatter plain = new PlainTextFormatter();
        LogLevelRegistry registry = new LogLevelRegistry();
        StructuredLoggerFactory factory = new StructuredLoggerFactory(plain, registry);

        assertSame(plain, factory.getFormatter());
        assertSame(registry, factory.getLevelRegistry());

        StructuredLogger logger = factory.getLogger("com.demo.B");
        assertSame(plain, logger.getFormatter(), "Logger 应使用工厂配置的 formatter");
    }

    @Test
    @DisplayName("null formatter 应回退到默认 JsonLogFormatter")
    void testNullFormatterFallsBack() {
        StructuredLoggerFactory factory = new StructuredLoggerFactory(null);

        assertTrue(factory.getFormatter() instanceof JsonLogFormatter);
    }

    @Test
    @DisplayName("非法名称应抛出 IllegalArgumentException")
    void testInvalidNames() {
        StructuredLoggerFactory factory = new StructuredLoggerFactory();

        assertThrows(IllegalArgumentException.class, () -> factory.getLogger((String) null));
        assertThrows(IllegalArgumentException.class, () -> factory.getLogger(""));
        assertThrows(IllegalArgumentException.class, () -> factory.getLogger((Class<?>) null));
    }

    @Test
    @DisplayName("工厂级别注册表应对所有 Logger 生效")
    void testFactoryRegistryAppliesToAllLoggers() {
        StructuredLoggerFactory factory = new StructuredLoggerFactory();
        factory.getLevelRegistry().setLevel("com.quiet", LogLevel.ERROR);

        StructuredLogger quiet = factory.getLogger("com.quiet.Job");
        StructuredLogger normal = factory.getLogger("com.normal.Job");

        assertFalse(quiet.isInfoEnabled(), "前缀覆盖应拦截 quiet Logger 的 INFO");
        assertTrue(normal.isInfoEnabled(), "未覆盖的 Logger 不受影响");
    }
}
