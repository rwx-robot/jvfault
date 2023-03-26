package com.jvfault.logging;

/**
 * 日志格式化器 SPI —— 将 {@link LogEntry} 渲染为单行文本
 *
 * <p>实现要求：输出必须是单行文本（内部自行转义换行符等控制字符），
 * 以保证 slf4j 底端每条日志事件只产生一行输出。
 *
 * <p>内置实现：{@link JsonLogFormatter}（默认）、{@link PlainTextFormatter}。
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
@FunctionalInterface
public interface LogFormatter {

    /**
     * 将日志条目渲染为单行文本
     *
     * @param entry 日志条目（不为 null）
     * @return 单行文本
     */
    String format(LogEntry entry);
}
