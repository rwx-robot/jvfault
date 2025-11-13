package com.jvfault.example.v040;

import com.jvfault.core.annotation.Component;
import com.jvfault.web.WebApplication;
import com.jvfault.web.annotation.Controller;
import com.jvfault.web.annotation.Get;
import com.jvfault.web.annotation.Param;
import com.jvfault.web.annotation.Post;
import com.jvfault.web.annotation.Body;
import com.jvfault.web.http.HttpContext;
import com.jvfault.web.http.HttpRequest;
import com.jvfault.web.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class Application {

    @Component
    @Controller("/api/users")
    static class UserController {
        @Get
        public String list() {
            return "[alice, bob]";
        }

        @Get(":id")
        public Map<String, Object> detail(@Param("id") long id) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", id);
            user.put("name", "user-" + id);
            return user;
        }

        @Post
        public String create(@Body User body) {
            return "created: " + body.name;
        }
    }

    static class User {
        public String name;
    }

    /** 极简内存 HTTP 上下文（真实容器由 Servlet/Jetty 桥接） */
    static class StubRequest implements HttpRequest {
        String method = "GET";
        String path = "/";
        final Map<String, String> pathParams = new LinkedHashMap<>();
        final Map<String, String> queryParams = new LinkedHashMap<>();
        byte[] body = new byte[0];

        @Override public String getMethod() { return method; }
        @Override public String getPath() { return path; }
        @Override public Map<String, String> getPathParams() { return pathParams; }
        @Override public Map<String, String> getQueryParams() { return queryParams; }
        @Override public String getHeader(String name) { return null; }
        @Override public byte[] getBody() { return body; }
        @Override public void setBody(byte[] body) { this.body = body; }
    }

    static class StubResponse implements HttpResponse {
        int status;
        byte[] body = new byte[0];
        final Map<String, String> headers = new LinkedHashMap<>();
        @Override public int getStatus() { return status; }
        @Override public void setStatus(int status) { this.status = status; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public byte[] getBody() { return body; }
        @Override public void setBody(byte[] body) { this.body = body; }
    }

    public static void main(String[] args) {
        System.out.println("== jvfault v0.4.0: Web ==");
        WebApplication app = new WebApplication(new com.jvfault.core.container.DefaultBeanRegistry());
        app.registerController(new UserController());

        request(app, "GET", "/api/users", null);
        request(app, "GET", "/api/users/42", null);
        request(app, "POST", "/api/users", "{\"name\":\"alice\"}");
        request(app, "GET", "/api/missing", null);
        System.out.println("== 完成 ==");
    }

    private static void request(WebApplication app, String method, String path, String body) {
        StubRequest request = new StubRequest();
        request.method = method;
        request.path = path;
        if (body != null) {
            request.body = body.getBytes();
        }
        StubResponse response = new StubResponse();
        app.dispatch(new HttpContext(request, response));
        System.out.println("  " + method + " " + path + " -> " + response.status
                + " " + new String(response.body));
    }
}
