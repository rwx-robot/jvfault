package com.jvfault.platform.reactive;

import com.jvfault.web.http.HttpContext;
import com.jvfault.web.http.HttpRequest;
import com.jvfault.web.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * platform-reactive 核心测试
 *
 * @since v0.8.0 (2022)
 */
@DisplayName("Platform-Reactive 测试")
class ReactiveModuleTest {

    static class StubRequest implements HttpRequest {
        final Map<String, String> pathParams = new LinkedHashMap<>();
        final Map<String, String> queryParams = new LinkedHashMap<>();
        @Override public String getMethod() { return "GET"; }
        @Override public String getPath() { return "/"; }
        @Override public Map<String, String> getPathParams() { return pathParams; }
        @Override public Map<String, String> getQueryParams() { return queryParams; }
        @Override public String getHeader(String name) { return null; }
        @Override public byte[] getBody() { return new byte[0]; }
        @Override public void setBody(byte[] body) { }
    }

    static class StubResponse implements HttpResponse {
        private int status;
        private byte[] body = new byte[0];
        private final Map<String, String> headers = new LinkedHashMap<>();
        @Override public int getStatus() { return status; }
        @Override public void setStatus(int status) { this.status = status; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public byte[] getBody() { return body; }
        @Override public void setBody(byte[] body) { this.body = body; }
    }

    private final ReactiveResultHandler handler = new ReactiveResultHandler();

    @Test
    @DisplayName("Mono 结果渲染为 JSON")
    void testMono() {
        HttpContext ctx = new HttpContext(new StubRequest(), new StubResponse());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        handler.render(Mono.just(payload), ctx);

        assertEquals(200, ctx.getResponse().getStatus());
        assertTrue(new String(ctx.getResponse().getBody(), StandardCharsets.UTF_8).contains("\"ok\":true"));
    }

    @Test
    @DisplayName("空 Mono 渲染 204")
    void testEmptyMono() {
        HttpContext ctx = new HttpContext(new StubRequest(), new StubResponse());
        handler.render(Mono.<String>empty(), ctx);
        assertEquals(204, ctx.getResponse().getStatus());
    }

    @Test
    @DisplayName("Flux 结果渲染为 NDJSON")
    void testFlux() {
        HttpContext ctx = new HttpContext(new StubRequest(), new StubResponse());
        handler.render(Flux.just("a", "b", "c"), ctx);

        assertEquals(200, ctx.getResponse().getStatus());
        assertEquals("application/x-ndjson", ctx.getResponse().getHeader("Content-Type"));
        String body = new String(ctx.getResponse().getBody(), StandardCharsets.UTF_8);
        assertEquals("\"a\"\n\"b\"\n\"c\"\n", body);
    }

    @Test
    @DisplayName("错误信号向上传播")
    void testErrorPropagates() {
        HttpContext ctx = new HttpContext(new StubRequest(), new StubResponse());
        assertThrows(IllegalStateException.class,
                () -> handler.render(Mono.error(new IllegalStateException("boom")), ctx));
    }

    @Test
    @DisplayName("非响应式结果拒绝")
    void testNonReactiveRejected() {
        HttpContext ctx = new HttpContext(new StubRequest(), new StubResponse());
        assertThrows(IllegalArgumentException.class, () -> handler.render("plain", ctx));
    }
}
