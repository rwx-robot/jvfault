package com.jvfault.logging;

import org.slf4j.Logger;

/**
 * 结构化 Logger —— 包装 {@link Logger org.slf4j.Logger}，将键值对渲染为单行文本后输出
 *
 * <p>内部流程：
 * <ol>
 *   <li>级别判断：先查 {@link LogLevelRegistry}（前缀覆盖），再透传 slf4j 的 {@code isXxxEnabled()}</li>
 *   <li>构建 {@link LogEntry}（traceId/spanId 从 MDC 读取）</li>
 *   <li>交给 {@link LogFormatter} 渲染为单行文本</li>
 *   <li>映射到底层 slf4j 对应级别输出</li>
 * </ol>
 *
 * <p>使用示例：
 * <pre>{@code
 * StructuredLogger logger = factory.getLogger(OrderService.class);
 * logger.info("订单已创建", KeyValuePair.of("orderId", 42), KeyValuePair.of("city", "上海"));
 * }</pre>
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public final class StructuredLogger {

    private final Logger logger;
    private final LogFormatter formatter;
    private final LogLevelRegistry levelRegistry;

    /**
     * 包内构造：由 {@link StructuredLoggerFactory} 创建
     *
     * @param logger        底层 slf4j Logger
     * @param formatter     格式化器
     * @param levelRegistry 级别注册表（可为 null，表示不启用运行时级别覆盖）
     */
    StructuredLogger(Logger logger, LogFormatter formatter, LogLevelRegistry levelRegistry) {
        this.logger = logger;
        this.formatter = formatter;
        this.levelRegistry = levelRegistry;
    }

    // ============ 结构化输出 API ============

    /**
     * 输出 INFO 级别结构化日志
     *
     * @param message 日志消息
     * @param kvs     结构化键值对（可变参数）
     */
    public void info(String message, KeyValuePair... kvs) {
        log(LogLevel.INFO, message, kvs);
    }

    /**
     * 输出 WARN 级别结构化日志
     *
     * @param message 日志消息
     * @param kvs     结构化键值对（可变参数）
     */
    public void warn(String message, KeyValuePair... kvs) {
        log(LogLevel.WARN, message, kvs);
    }

    /**
     * 输出 ERROR 级别结构化日志
     *
     * @param message 日志消息
     * @param kvs     结构化键值对（可变参数）
     */
    public void error(String message, KeyValuePair... kvs) {
        log(LogLevel.ERROR, message, kvs);
    }

    /**
     * 输出 DEBUG 级别结构化日志
     *
     * @param message 日志消息
     * @param kvs     结构化键值对（可变参数）
     */
    public void debug(String message, KeyValuePair... kvs) {
        log(LogLevel.DEBUG, message, kvs);
    }

    // ============ 级别判断（透传 + 注册表覆盖） ============

    public boolean isTraceEnabled() {
        return levelEnabled(LogLevel.TRACE) && logger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return levelEnabled(LogLevel.DEBUG) && logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return levelEnabled(LogLevel.INFO) && logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return levelEnabled(LogLevel.WARN) && logger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return levelEnabled(LogLevel.ERROR) && logger.isErrorEnabled();
    }

    // ============ 内省 ============

    /**
     * Logger 名称
     */
    public String getName() {
        return logger.getName();
    }

    /**
     * 当前使用的格式化器
     */
    public LogFormatter getFormatter() {
        return formatter;
    }

    // ============ 内部实现 ============

    private void log(LogLevel level, String message, KeyValuePair[] kvs) {
        // 先按注册表与底层 slf4j 级别拦截，被拦截的日志不进入格式化流程
        if (!levelEnabled(level) || !slf4jEnabled(level)) {
            return;
        }
        String line = render(level, message, kvs);
        switch (level) {
            case DEBUG:
                logger.debug(line);
                break;
            case INFO:
                logger.info(line);
                break;
            case WARN:
                logger.warn(line);
                break;
            case ERROR:
                logger.error(line);
                break;
            case TRACE:
            default:
                logger.trace(line);
                break;
        }
    }

    private boolean slf4jEnabled(LogLevel level) {
        switch (level) {
            case DEBUG:   return logger.isDebugEnabled();
            case INFO:    return logger.isInfoEnabled();
            case WARN:    return logger.isWarnEnabled();
            case ERROR:   return logger.isErrorEnabled();
            case TRACE:
            default:      return logger.isTraceEnabled();
        }
    }

    /**
     * 注册表覆盖判断：null 生效级别表示未覆盖，交由 slf4j 决定
     */
    private boolean levelEnabled(LogLevel level) {
        if (levelRegistry == null) {
            return true;
        }
        return level.enabledAt(levelRegistry.getEffectiveLevel(logger.getName()));
    }

    private String render(LogLevel level, String message, KeyValuePair[] kvs) {
        LogEntry.Builder builder = LogEntry.builder()
                .level(level)
                .loggerName(logger.getName())
                .message(message)
                .fromMdc();
        if (kvs != null) {
            for (KeyValuePair kv : kvs) {
                builder.keyValue(kv.getKey(), kv.getValue());
            }
        }
        return formatter.format(builder.build());
    }
}
