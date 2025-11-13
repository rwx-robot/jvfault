package com.jvfault.tracing;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Span 构建器（链式 API）
 * 对应 OpenTelemetry: SpanBuilder
 *
 * <p>父上下文解析规则：显式 {@link #setParent(TraceContext)} 优先；
 * 未设置时回退到 {@link Tracer} 当前活跃 Span；无活跃 Span 时创建 root Span。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public final class SpanBuilder {

    private final Tracer tracer;
    private final String name;
    private TraceContext parent;
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    SpanBuilder(Tracer tracer, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("span name 不能为空");
        }
        this.tracer = tracer;
        this.name = name;
    }

    /**
     * 显式指定父上下文（跨进程传播场景）
     */
    public SpanBuilder setParent(TraceContext parent) {
        this.parent = parent;
        return this;
    }

    /**
     * 设置初始属性
     */
    public SpanBuilder setAttribute(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            attributes.put(key, value);
        }
        return this;
    }

    /**
     * 创建并启动 Span（startNanos = System.nanoTime()）
     *
     * <p>子 Span 继承父上下文的 traceId/traceFlags/traceState，
     * parentId 为父上下文的 spanId，并生成新的 spanId。
     */
    public Span start() {
        TraceContext parentContext = parent != null ? parent : tracer.currentContext();
        TraceContext context;
        if (parentContext == null) {
            // root Span：新 traceId，无 parentId，默认采样
            context = new TraceContext(IdGenerator.generateTraceId(), null,
                    IdGenerator.generateSpanId(), TraceContext.FLAG_SAMPLED, null);
        } else {
            context = new TraceContext(parentContext.getTraceId(), parentContext.getSpanId(),
                    IdGenerator.generateSpanId(), parentContext.getTraceFlags(), parentContext.getTraceState());
        }
        Span span = new Span(name, context, System.nanoTime());
        span.setEndListener(tracer::exportEnded);
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            span.setAttribute(attribute.getKey(), attribute.getValue());
        }
        return span;
    }
}
