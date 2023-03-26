package com.jvfault.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-exception 核心测试
 *
 * @since v0.2.0 (2016)
 */
@DisplayName("Exception 模块测试")
class ExceptionModuleTest {

    // ============ 测试处理器 ============

    static class TestHandlers {
        ProblemDetail last;

        @ExceptionHandler(NotFoundException.class)
        public ProblemDetail onNotFound(NotFoundException ex) {
            last = ProblemDetail.forStatusAndDetail(404, "handled:" + ex.getMessage()).property("code", "USER_NOT_FOUND");
            return last;
        }

        @ExceptionHandler({BadRequestException.class, ConflictException.class})
        public String onClientError(Throwable ex) {
            return "client-error:" + ex.getMessage();
        }

        @ExceptionHandler(IllegalStateException.class)
        public void onIllegalState(IllegalStateException ex) {
            last = ProblemDetail.forStatusAndDetail(418, "teapot");
        }
    }

    static class BaseHandlers {
        @ExceptionHandler(RuntimeException.class)
        public ProblemDetail onRuntime(RuntimeException ex) {
            return ProblemDetail.forStatusAndDetail(500, "base-runtime");
        }
    }

    static class SpecificHandlers extends BaseHandlers {
        @ExceptionHandler(IllegalArgumentException.class)
        public ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
            return ProblemDetail.forStatusAndDetail(422, "specific-ia");
        }
    }

    // ============ 异常层次 ============

    @Test
    @DisplayName("HTTP 异常携带正确状态码与 ProblemDetail")
    void testExceptionHierarchy() {
        assertEquals(400, HttpException.badRequest("bad input").getStatus());
        assertEquals(401, HttpException.unauthorized("no token").getStatus());
        assertEquals(403, HttpException.forbidden("no role").getStatus());
        assertEquals(404, HttpException.notFound("user 1").getStatus());
        assertEquals(409, HttpException.conflict("dup").getStatus());
        assertEquals(500, HttpException.internalServerError("boom").getStatus());
        assertEquals(503, HttpException.serviceUnavailable("maintenance").getStatus());

        HttpException ex = HttpException.notFound("user 1");
        assertEquals("Not Found", ex.getProblem().getTitle());
        assertEquals("user 1", ex.getProblem().getDetail());
        assertEquals("user 1", ex.getMessage());
    }

    @Test
    @DisplayName("异常支持附加响应头")
    void testExceptionHeaders() {
        HttpException ex = HttpException.unauthorized("login required")
                .header("WWW-Authenticate", "Bearer realm=\"api\"");
        assertEquals("Bearer realm=\"api\"", ex.getHeaders().get("WWW-Authenticate"));
    }

    // ============ ProblemDetail ============

    @Test
    @DisplayName("ProblemDetail JSON 序列化与转义")
    void testProblemDetailJson() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(404, "line1\nline2 \"quoted\"")
                .property("timestamp", 12345L)
                .property("tags", java.util.Arrays.asList("a", "b"));

        String json = pd.toJson();
        assertTrue(json.contains("\"status\":404"));
        assertTrue(json.contains("\"title\":\"Not Found\""));
        assertTrue(json.contains("line1\\nline2 \\\"quoted\\\""));
        assertTrue(json.contains("\"timestamp\":12345"));
        assertTrue(json.contains("\"tags\":[\"a\",\"b\"]"));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    @DisplayName("ProblemDetail.of 映射 HttpException 与普通异常")
    void testProblemDetailOf() {
        assertEquals(404, ProblemDetail.of(HttpException.notFound("x")).getStatus());
        ProblemDetail generic = ProblemDetail.of(new RuntimeException("boom"));
        assertEquals(500, generic.getStatus());
        assertEquals("boom", generic.getDetail());
    }

    // ============ 处理器注册与解析 ============

    @Test
    @DisplayName("registry: 精确匹配与自定义 handler")
    void testRegistryExactMatch() throws Throwable {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        TestHandlers handlers = new TestHandlers();
        registry.register(handlers);

        ExceptionHandlerRegistry.HandlerMethod hm =
                registry.resolve(HttpException.notFound("user 9")).orElse(null);
        assertNotNull(hm);
        Object result = hm.invoke(HttpException.notFound("user 9"), null);
        assertTrue(result instanceof ProblemDetail);
        assertEquals("handled:user 9", ((ProblemDetail) result).getDetail());
        assertEquals("USER_NOT_FOUND", ((ProblemDetail) result).getProperties().get("code"));
    }

    @Test
    @DisplayName("registry: 多类型 handler 与 String 返回值")
    void testRegistryMultiTypeAndStringReturn() throws Throwable {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        registry.register(new TestHandlers());

        ExceptionHandlerRegistry.HandlerMethod hm =
                registry.resolve(HttpException.conflict("dup user")).orElse(null);
        assertNotNull(hm);
        Object result = hm.invoke(HttpException.conflict("dup user"), null);
        assertEquals("client-error:dup user", result);
    }

    @Test
    @DisplayName("registry: 无匹配返回 empty")
    void testRegistryNoMatch() {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        registry.register(new TestHandlers());
        assertFalse(registry.resolve(new NullPointerException("npe")).isPresent());
    }

    @Test
    @DisplayName("registry: 最具体处理器胜出（继承距离）")
    void testRegistryMostSpecificWins() {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        SpecificHandlers handlers = new SpecificHandlers();
        registry.register(handlers);

        // IllegalArgumentException -> 具体处理器 (422)
        assertTrue(registry.resolve(new IllegalArgumentException("x")).isPresent());
        // NumberFormatException extends IllegalArgumentException -> 仍是具体处理器
        assertTrue(registry.resolve(new NumberFormatException("n")).isPresent());
        // IllegalStateException extends RuntimeException -> 基类处理器
        assertTrue(registry.resolve(new IllegalStateException("s")).isPresent());
    }

    // ============ ExceptionAdvice ============

    @Test
    @DisplayName("advice: fallback 500 与 HttpException 透传")
    void testAdviceFallback() {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        ExceptionAdvice advice = new ExceptionAdvice(registry);

        ProblemDetail unknown = advice.handle(new NullPointerException("npe!"));
        assertEquals(500, unknown.getStatus());
        assertEquals("npe!", unknown.getDetail());

        ProblemDetail http = advice.handle(new TooManyRequestsException("limit"));
        assertEquals(429, http.getStatus());
    }

    @Test
    @DisplayName("advice: 自定义 handler 接管")
    void testAdviceCustomHandler() {
        ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
        TestHandlers handlers = new TestHandlers();
        registry.register(handlers);
        ExceptionAdvice advice = new ExceptionAdvice(registry);

        ProblemDetail pd = advice.handle(HttpException.notFound("u1"));
        assertEquals(404, pd.getStatus());
        assertEquals("handled:u1", pd.getDetail());
    }
}
