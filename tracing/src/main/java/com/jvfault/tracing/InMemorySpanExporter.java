package com.jvfault.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存 Span 导出器 - 有界环形缓冲，测试与调试用。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class InMemorySpanExporter implements SpanExporter {

    private final List<Span> spans = new ArrayList<>();
    private final int maxSize;

    public InMemorySpanExporter() {
        this(1024);
    }

    public InMemorySpanExporter(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized void export(List<Span> batch) {
        for (Span span : batch) {
            if (spans.size() >= maxSize) {
                spans.remove(0);
            }
            spans.add(span);
        }
    }

    public synchronized List<Span> getFinishedSpans() {
        return Collections.unmodifiableList(new ArrayList<>(spans));
    }

    public synchronized void clear() {
        spans.clear();
    }
}
