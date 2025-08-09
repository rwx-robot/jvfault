package com.jvfault.tests;

import com.jvfault.core.annotation.Component;
import com.jvfault.security.crypto.PasswordHasher;
import com.jvfault.security.guard.JwtGuard;
import com.jvfault.security.jwt.JwtService;
import com.jvfault.web.WebApplication;
import com.jvfault.web.annotation.Controller;
import com.jvfault.web.annotation.Get;
import com.jvfault.web.annotation.Param;
import com.jvfault.platform.servlet.JvfaultServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端集成测试：真实 HTTP 服务器（Jetty）→ Servlet 桥接 →
 * WebApplication 路由/守卫 → JWT 鉴权。
 *
 * @since v1.0.0 (2026)
 */
@DisplayName("端到端集成: Jetty + Servlet 桥接 + 路由 + JWT")
class E2eIntegrationTest {

    static class StubResponse extends HttpServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) {
            // 占位：由 JvfaultServlet 构造器注入的 application 处理
        }
    }

    @Component
    @Controller("/api")
    static class AdminController {

        @Get("health")
        public Map<String, Object> health() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "UP");
            return body;
        }

        @Get("admin/secret")
        public Map<String, Object> secret() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("data", "classified");
            return body;
        }

        @Get("admin/users/:id")
        public Map<String, Object> user(@Param("id") long id) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", id);
            body.put("name", "user-" + id);
            return body;
        }
    }

    static JettyHandle jetty;
    static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    static JwtService jwt;
    static PasswordHasher hasher;

    /** Jetty 生命周期句柄 */
    static final class JettyHandle {
        final org.eclipse.jetty.server.Server server;
        final int port;

        JettyHandle(org.eclipse.jetty.server.Server server, int port) {
            this.server = server;
            this.port = port;
        }
    }

    @BeforeAll
    static void startServer() throws Exception {
        jwt = new JwtService("e2e-test-secret-key", 300);
        hasher = new PasswordHasher();

        WebApplication app = new WebApplication(null);
        app.registerController(new AdminController());
        // 守卫：/api/health 公开；其余 /api/admin/** 需要 admin 角色
        app.addGuard(ctx -> {
            String path = ctx.getPath();
            if (path.equals("/api/health")) {
                return true;
            }
            JwtGuard adminGuard = new JwtGuard(jwt, "admin");
            return adminGuard.canActivate(ctx);
        });

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(0);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        HttpServlet dispatchServlet = new JvfaultServlet(app);
        context.addServlet(new ServletHolder(dispatchServlet), "/*");
        server.setHandler(context);
        server.start();
        jetty = new JettyHandle(server, server.getURI().getPort());
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (jetty != null) {
            jetty.server.stop();
        }
    }

    private static HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + jetty.port + path))
                .timeout(Duration.ofSeconds(5));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return HTTP.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("公开端点: /api/health 返回 200")
    void testPublicEndpoint() throws Exception {
        HttpResponse<String> response = get("/api/health", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    @DisplayName("受保护端点: 无令牌 403")
    void testProtectedWithoutToken() throws Exception {
        HttpResponse<String> response = get("/api/admin/secret", null);
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("受保护端点: 合法令牌 200 并返回数据")
    void testProtectedWithToken() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "admin-1");
        claims.put("role", "admin");
        String token = jwt.issue(claims);

        HttpResponse<String> response = get("/api/admin/secret", token);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("classified"));
    }

    @Test
    @DisplayName("角色不符: user 令牌访问 admin 端点 403")
    void testWrongRole() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "u-1");
        claims.put("role", "user");
        String token = jwt.issue(claims);

        HttpResponse<String> response = get("/api/admin/secret", token);
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("路径参数经真实 HTTP 绑定")
    void testPathParamOverHttp() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("role", "admin");
        String token = jwt.issue(claims);

        HttpResponse<String> response = get("/api/admin/users/77", token);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":77"));
    }

    @Test
    @DisplayName("未知路由: 真实 HTTP 404")
    void testNotFoundOverHttp() throws Exception {
        HttpResponse<String> response = get("/api/nope", null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("口令哈希端到端语义（签发链路依赖）")
    void testPasswordSemantics() {
        String stored = hasher.hash("P@ssw0rd!");
        assertTrue(hasher.verify("P@ssw0rd!", stored));
        assertFalse(hasher.verify("wrong", stored));
    }
}
