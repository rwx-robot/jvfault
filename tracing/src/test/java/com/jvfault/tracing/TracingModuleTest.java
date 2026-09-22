package com.jvfault.tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-tracing 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("Tracing 模块测试")
class TracingModuleTest {

    // ============ IdGenerator ============

    @Test
    @DisplayName("IdGenerator 生成 16/32 位 hex")
    void testIdGenerator() {
        String spanId = IdGenerator.generateSpanId();
        String traceId = IdGenerator.generateTraceId();
        assertTrue(spanId.matches("[0-9a-f]{16}"), "spanId 16 hex: " + spanId);
        assertTrue(traceId.matches("[0-9a-f]{32}"), "traceId 32 hex: " + traceId);
        assertNotEquals(spanId, IdGenerator.generateSpanId());
    }

    // ============ W3C 传播 ============

    @Test
    @DisplayName("traceparent 注入/提取往返")
    void testPropagationRoundtrip() {
        Tracer tracer = new Tracer();
        Span span = tracer.startSpan("root");

        Map<String, String> carrier = new HashMap<>();
        W3CPropagation.inject(span.getContext(), carrier);
        assertTrue(carrier.get("traceparent").startsWith("00-"));

        TraceContext extracted = W3CPropagation.extract(carrier);
        assertNotNull(extracted);
        assertEquals(span.getContext().getTraceId(), extracted.getTraceId());
        assertEquals(span.getContext().getSpanId(), extracted.getSpanId());
    }

    @Test
    @DisplayName("非法 traceparent 容错返回 null")
    void testPropagationInvalid() {
        Map<String, String> bad1 = new HashMap<>();
        bad1.put("traceparent", "not-a-valid-header");
        assertNull(W3CPropagation.extract(bad1));

        Map<String, String> bad2 = new HashMap<>();
        bad2.put("traceparent", "00-zzz-yyy-01");
        assertNull(W3CPropagation.extract(bad2));

        assertNull(W3CPropagation.extract(new HashMap<String, String>()));
    }

    // ============ Span 与 Tracer ============

    @Test
    @DisplayName("span 父子关系与耗时")
    void testSpanParenting() {
        Tracer tracer = new Tracer();
        Span parent = tracer.startSpan("parent");
        Span child;
        try (Tracer.SpanInScope scope = tracer.withActiveSpan(parent)) {
            child = tracer.startSpan("child"); // 父 = 当前活跃 span
        }

        assertEquals(parent.getContext().getTraceId(), child.getContext().getTraceId(), "子 span 应继承 traceId");
        assertEquals(parent.getContext().getSpanId(), child.getContext().getParentId());

        child.end();
        parent.end();
        assertTrue(child.elapsedMs() >= 0);
        assertTrue(parent.getEndNanos() >= parent.getStartNanos());
    }

    @Test
    @DisplayName("withActiveSpan 作用域与 currentSpan")
    void testActiveSpanScope() throws Exception {
        Tracer tracer = new Tracer();
        assertNull(tracer.currentSpan());

        Span span = tracer.startSpan("op");
        try (Tracer.SpanInScope scope = tracer.withActiveSpan(span)) {
            assertSame(span, tracer.currentSpan());
        }
        assertNull(tracer.currentSpan(), "close 后上下文应清空");
        span.end();
    }

    @Test
    @DisplayName("span 属性、事件与状态")
    void testSpanAttributes() {
        Tracer tracer = new Tracer();
        Span span = tracer.spanBuilder("query")
                .setAttribute("db.statement", "SELECT 1")
                .start();

        span.addEvent("cache-miss");
        span.setStatus(Span.Status.ERROR);
        span.end();

        assertEquals("query", span.getName());
        assertEquals("SELECT 1", span.getAttributes().get("db.statement"));
        assertEquals(1, span.getEvents().size());
        assertEquals(Span.Status.ERROR, span.getStatus());
    }

    // ============ 导出 ============

    @Test
    @DisplayName("InMemorySpanExporter 接收完成的 span")
    void testInMemoryExporter() {
        Tracer tracer = new Tracer();
        InMemorySpanExporter exporter = new InMemorySpanExporter();
        tracer.addExporter(exporter);

        Span span = tracer.startSpan("op-1");
        span.end();

        List<Span> finished = exporter.getFinishedSpans();
        assertEquals(1, finished.size());
        assertEquals("op-1", finished.get(0).getName());
    }

    @Test
    @DisplayName("未 end 的 span 不导出")
    void testNoExportWithoutEnd() {
        Tracer tracer = new Tracer();
        InMemorySpanExporter exporter = new InMemorySpanExporter();
        tracer.addExporter(exporter);

        tracer.startSpan("never-ended");
        assertEquals(0, exporter.getFinishedSpans().size());
    }

    @Test
    @DisplayName("导出器异常不影响主流程")
    void testExporterFailureTolerated() {
        Tracer tracer = new Tracer();
        tracer.addExporter(spans -> {
            throw new RuntimeException("export down");
        });
        InMemorySpanExporter healthy = new InMemorySpanExporter();
        tracer.addExporter(healthy);

        tracer.startSpan("resilient").end();
        assertEquals(1, healthy.getFinishedSpans().size(), "健康导出器应仍收到 span");
    }
}
