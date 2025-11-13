package com.jvfault.web;

import com.jvfault.core.annotation.Component;
import com.jvfault.web.annotation.Body;
import com.jvfault.web.annotation.Controller;
import com.jvfault.web.annotation.Get;
import com.jvfault.web.annotation.Param;
import com.jvfault.web.annotation.Post;
import com.jvfault.web.annotation.Query;
import com.jvfault.web.http.HttpContext;
import com.jvfault.web.pipeline.RequestPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-web 核心测试（Mock HTTP 上下文）
 *
 * @since v0.2.0 (2016)
 */
@DisplayName("Web 模块测试")
class WebModuleTest {

    // ============ Mock HTTP ============

    static class MockRequest implements com.jvfault.web.http.HttpRequest {
        String method = "GET";
        String path = "/";
        final Map<String, String> pathParams = new LinkedHashMap<>();
        final Map<String, String> queryParams = new LinkedHashMap<>();
        final Map<String, String> headers = new LinkedHashMap<>();
        byte[] body = new byte[0];

        @Override public String getMethod() { return method; }
        @Override public String getPath() { return path; }
        @Override public Map<String, String> getPathParams() { return pathParams; }
        @Override public Map<String, String> getQueryParams() { return queryParams; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public byte[] getBody() { return body; }
        @Override public void setBody(byte[] body) { this.body = body; }
    }

    static class MockResponse implements com.jvfault.web.http.HttpResponse {
        int status = 200;
        final Map<String, String> headers = new LinkedHashMap<>();
        byte[] body = new byte[0];

        @Override public int getStatus() { return status; }
        @Override public void setStatus(int status) { this.status = status; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public byte[] getBody() { return body; }
        @Override public void setBody(byte[] body) { this.body = body; }
    }

    private MockRequest request;
    private MockResponse response;
    private WebApplication app;

    @BeforeEach
    void setUp() {
        request = new MockRequest();
        response = new MockResponse();
        app = new WebApplication(null);
        app.registerController(new UserController());
    }

    private HttpContext context() {
        return new HttpContext(request, response);
    }

    // ============ 测试控制器 ============

    @Component
    @Controller("/users")
    static class UserController {

        @Get
        public String list(@Query("page") int page) {
            return "users-page-" + page;
        }

        @Get(":id")
        public String detail(@Param("id") long id) {
            return "user-" + id;
        }

        @Post
        public User create(@Body User body) {
            return body;
        }

        @Get(":id/orders/:orderId")
        public String order(@Param("id") long id, @Param("orderId") String orderId,
                            @com.jvfault.web.annotation.Header("X-Trace") String trace) {
            return "order-" + id + "-" + orderId + "-" + (trace == null ? "-" : trace);
        }

        @Get("search")
        public HttpContext search(HttpContext ctx) {
            ctx.json(200, java.util.Collections.<String, Object>singletonMap("q", ctx.getQuery("q")));
            return null;
        }

        @Get("boom")
        public String boom() {
            throw new com.jvfault.exception.NotFoundException("user 999");
        }
    }

    static class User {
        public String name;
        public int age;
    }

    // ============ 路由与绑定 ============

    @Test
    @DisplayName("路由解析与查询参数绑定")
    void testRoutingAndQueryBinding() {
        request.method = "GET";
        request.path = "/users";
        request.queryParams.put("page", "3");
        app.dispatch(context());
        assertEquals(200, response.status);
        assertEquals("users-page-3", new String(response.body, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("路径参数绑定")
    void testPathParamBinding() {
        request.method = "GET";
        request.path = "/users/42";
        app.dispatch(context());
        assertEquals("user-42", new String(response.body, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("多路径段参数与请求头绑定")
    void testMultiParamsAndHeader() {
        request.method = "GET";
        request.path = "/users/7/orders/A-100";
        request.headers.put("X-Trace", "tid-9");
        app.dispatch(context());
        assertEquals("order-7-A-100-tid-9", new String(response.body, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("@Body JSON 反序列化与回写")
    void testBodyBinding() {
        request.method = "POST";
        request.path = "/users";
        request.body = "{\"name\":\"Alice\",\"age\":30}".getBytes(StandardCharsets.UTF_8);
        app.dispatch(context());
        assertEquals(200, response.status);
        assertTrue(new String(response.body, StandardCharsets.UTF_8).contains("Alice"));
        assertTrue(response.headers.get("Content-Type").contains("application/json"));
    }

    @Test
    @DisplayName("HttpContext 注入与手动渲染")
    void testHttpContextInjection() {
        request.method = "GET";
        request.path = "/users/search";
        request.queryParams.put("q", "alice");
        app.dispatch(context());
        assertTrue(new String(response.body, StandardCharsets.UTF_8).contains("alice"));
    }

    @Test
    @DisplayName("404 与 405 映射")
    void testNotFoundAndMethodNotAllowed() {
        request.method = "GET";
        request.path = "/nope";
        app.dispatch(context());
        assertEquals(404, response.status);

        request.method = "DELETE";
        request.path = "/users/1";
        MockResponse resp2 = new MockResponse();
        app.dispatch(new HttpContext(request, resp2));
        assertEquals(405, resp2.status);
    }

    @Test
    @DisplayName("HttpException 经全局解析器转 ProblemDetail")
    void testExceptionMapping() {
        request.method = "GET";
        request.path = "/users/boom";
        app.dispatch(context());
        assertEquals(404, response.status);
        String body = new String(response.body, StandardCharsets.UTF_8);
        assertTrue(body.contains("user 999"), body);
        assertTrue(body.contains("\"status\":404"));
    }

    // ============ 管道 ============

    @Test
    @DisplayName("Guard 拒绝返回 403")
    void testGuardReject() {
        app.addGuard(ctx -> false);
        request.method = "GET";
        request.path = "/users/1";
        app.dispatch(context());
        assertEquals(403, response.status);
    }

    @Test
    @DisplayName("Interceptor 洋葱模型顺序")
    void testInterceptorOrder() throws Exception {
        java.util.List<String> calls = new java.util.ArrayList<>();
        RequestPipeline pipeline = new RequestPipeline();
        pipeline.addInterceptor((ctx, next) -> {
            calls.add("outer-before");
            Object r = next.invoke(ctx);
            calls.add("outer-after");
            return r;
        });
        pipeline.addInterceptor((ctx, next) -> {
            calls.add("inner-before");
            return next.invoke(ctx);
        });
        Object result = pipeline.proceed(null, ctx -> {
            calls.add("target");
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(java.util.Arrays.asList("outer-before", "inner-before", "target", "outer-after"),
                calls);
    }

    @Test
    @DisplayName("路由数量统计")
    void testRouteCount() {
        assertEquals(6, app.getRouter().routeCount());
    }
}
