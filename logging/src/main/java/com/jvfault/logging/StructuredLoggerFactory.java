package com.jvfault.logging;

import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 结构化 Logger 工厂 —— 创建并缓存 {@link StructuredLogger}
 *
 * <p>同名的 Logger 返回同一实例（与 slf4j 语义一致）；
 * formatter 与 {@link LogLevelRegistry} 在工厂级别配置，对所有 Logger 生效。
 *
 * <p>使用示例：
 * <pre>{@code
 * StructuredLoggerFactory factory = new StructuredLoggerFactory(); // 默认 JsonLogFormatter
 * StructuredLogger logger = factory.getLogger(OrderService.class);
 * }</pre>
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public class StructuredLoggerFactory {

    private final LogFormatter formatter;
    private final LogLevelRegistry levelRegistry;
    private final ConcurrentMap<String, StructuredLogger> loggers = new ConcurrentHashMap<String, StructuredLogger>();

    /**
     * 默认构造：使用 {@link JsonLogFormatter} 与独立的 {@link LogLevelRegistry}
     */
    public StructuredLoggerFactory() {
        this(new JsonLogFormatter(), new LogLevelRegistry());
    }

    /**
     * 指定格式化器（null 时回退默认 {@link JsonLogFormatter}）
     */
    public StructuredLoggerFactory(LogFormatter formatter) {
        this(formatter, new LogLevelRegistry());
    }

    /**
     * 指定格式化器与级别注册表（null 时回退默认值）
     */
    public StructuredLoggerFactory(LogFormatter formatter, LogLevelRegistry levelRegistry) {
        this.formatter = formatter != null ? formatter : new JsonLogFormatter();
        this.levelRegistry = levelRegistry != null ? levelRegistry : new LogLevelRegistry();
    }

    /**
     * 按类型获取 Logger（名称为类的全限定名）
     */
    public StructuredLogger getLogger(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type 不能为空");
        }
        return getLogger(type.getName());
    }

    /**
     * 按名称获取 Logger（同名缓存复用）
     */
    public StructuredLogger getLogger(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("logger 名称不能为空");
        }
        return loggers.computeIfAbsent(name, n ->
                new StructuredLogger(LoggerFactory.getLogger(n), formatter, levelRegistry));
    }

    /**
     * 当前格式化器
     */
    public LogFormatter getFormatter() {
        return formatter;
    }

    /**
     * 级别注册表（可用于运行时动态调整级别）
     */
    public LogLevelRegistry getLevelRegistry() {
        return levelRegistry;
    }
}
