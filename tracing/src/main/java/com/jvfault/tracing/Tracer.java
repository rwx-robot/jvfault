package com.jvfault.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracer —— Span 创建入口与活跃上下文（ThreadLocal）管理
 * 对应 OpenTelemetry: Tracer
 *
 * <p>特性：
 * <ul>
 *   <li>{@link #startSpan(String)} 以当前活跃 Span 为父（无则创建 root Span）</li>
 *   <li>{@link #startSpan(String, TraceContext)} 以显式父上下文创建（跨进程传播场景）</li>
 *   <li>{@link #withActiveSpan(Span)} 返回 {@link SpanInScope}（实现 AutoCloseable，
 *       close 时自动恢复进入前的上下文，防止 ThreadLocal 泄漏）</li>
 *   <li>可注册多个 {@link SpanExporter}，Span end 时逐个导出（导出异常被记录并吞掉）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * try (Tracer.SpanInScope scope = tracer.withActiveSpan(span)) {
 *     tracer.currentSpan().addEvent("processing");
 * }
 * }</pre>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class Tracer {

    private static final Logger log = LoggerFactory.getLogger(Tracer.class);

    private final List<SpanExporter> exporters = new CopyOnWriteArrayList<SpanExporter>();
    private final ThreadLocal<Span> activeSpan = new ThreadLocal<Span>();

    // ============ 导出器注册 ============

    /**
     * 注册导出器（可注册多个），Span end 时逐个导出
     */
    public Tracer addExporter(SpanExporter exporter) {
        if (exporter == null) {
            throw new IllegalArgumentException("exporter 不能为空");
        }
        exporters.add(exporter);
        return this;
    }

    // ============ Span 创建 ============

    /**
     * 创建 Span：父 = 当前上下文栈顶（活跃 Span），无活跃 Span 时创建 root Span
     */
    public Span startSpan(String name) {
        return spanBuilder(name).start();
    }

    /**
     * 以显式父上下文创建 Span（跨进程传播场景）
     */
    public Span startSpan(String name, TraceContext parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent 不能为空");
        }
        return spanBuilder(name).setParent(parent).start();
    }

    /**
     * 链式构建 Span
     */
    public SpanBuilder spanBuilder(String name) {
        return new SpanBuilder(this, name);
    }

    // ============ 活跃上下文（ThreadLocal） ============

    /**
     * 当前活跃 Span（无则 null）
     */
    public Span currentSpan() {
        return activeSpan.get();
    }

    /**
     * 当前活跃 Span 的 traceId（无活跃 Span 时返回 null）
     */
    public String traceId() {
        Span span = currentSpan();
        return span != null ? span.getContext().getTraceId() : null;
    }

    /**
     * 将 Span 置为活跃上下文（传 null 表示清空）。
     *
     * <p>必须以 try-with-resources 使用：close 时恢复进入前的上下文。
     *
     * <pre>{@code
     * try (Tracer.SpanInScope scope = tracer.withActiveSpan(span)) { ... }
     * }</pre>
     */
    public SpanInScope withActiveSpan(Span span) {
        return new SpanInScope(this, activeSpan.get(), span);
    }

    // ============ 内部实现 ============

    /**
     * 包内：供 {@link SpanBuilder} 获取当前活跃上下文
     */
    TraceContext currentContext() {
        Span span = activeSpan.get();
        return span != null ? span.getContext() : null;
    }

    /**
     * 包内：Span end 回调 —— 向所有导出器导出（异常记录并吞掉）
     */
    void exportEnded(Span span) {
        if (exporters.isEmpty()) {
            return;
        }
        List<Span> batch = Collections.singletonList(span);
        for (SpanExporter exporter : exporters) {
            try {
                exporter.export(batch);
            } catch (RuntimeException e) {
                log.warn("Span exporter threw exception, span={}", span.getName(), e);
            }
        }
    }

    /**
     * 活跃 Span 作用域 —— 实现 {@link AutoCloseable}，
     * {@link #close()} 恢复进入前的上下文（ThreadLocal 自动清理语义）
     */
    public static final class SpanInScope implements AutoCloseable {

        private final Tracer tracer;
        private final Span previous;
        private boolean closed;

        private SpanInScope(Tracer tracer, Span previous, Span active) {
            this.tracer = tracer;
            this.previous = previous;
            tracer.activeSpan.set(active);
        }

        /**
         * 恢复进入本作用域前的上下文（幂等）
         */
        @Override
        public void close() {
            if (!closed) {
                closed = true;
                tracer.activeSpan.set(previous);
            }
        }
    }
}
