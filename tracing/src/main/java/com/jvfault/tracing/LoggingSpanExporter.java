package com.jvfault.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 日志 Span 导出器 - 以 debug 级别输出 Span 摘要。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class LoggingSpanExporter implements SpanExporter {

    private static final Logger log = LoggerFactory.getLogger(LoggingSpanExporter.class);

    @Override
    public void export(List<Span> spans) {
        for (Span span : spans) {
            log.debug("span={} traceId={} spanId={} elapsed={}ms status={}",
                    span.getName(),
                    span.getContext().getTraceId(),
                    span.getContext().getSpanId(),
                    span.elapsedMs(),
                    span.getStatus());
        }
    }
}
