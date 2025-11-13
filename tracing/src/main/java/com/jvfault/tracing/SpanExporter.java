package com.jvfault.tracing;

import java.util.List;

/**
 * Span 导出器 - Span 结束时接收完成的 Span 列表。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface SpanExporter {

    /**
     * 导出已完成（end）的 Span。
     */
    void export(List<Span> spans);
}
