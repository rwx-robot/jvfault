package com.jvfault.security;

import com.jvfault.web.http.HttpContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 测试辅助（Mock HTTP 上下文 + base64url）。
 */
public final class TestSupport {

    private TestSupport() {
    }

    public static String b64url(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** 便捷构造器 */
    public static final class MockRequestHelperHelper {
        public static HttpContext withBearer(String token) {
            return new MockRequestHelper().withHeader("Authorization", "Bearer " + token).toContext();
        }

        public static HttpContext empty() {
            return new MockRequestHelper().toContext();
        }
    }

    /** 供测试构造 HttpContext 的辅助请求 */
    public static final class MockRequestHelper {
        final Map<String, String> headers = new LinkedHashMap<>();

        public MockRequestHelper withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public HttpContext toContext() {
            HttpRequestStub request = new HttpRequestStub();
            request.headers.putAll(headers);
            return new HttpContext(request, new HttpResponseStub());
        }
    }

    public static final class HttpRequestStub implements com.jvfault.web.http.HttpRequest {
        final Map<String, String> headers = new LinkedHashMap<>();
        final Map<String, String> pathParams = new LinkedHashMap<>();
        final Map<String, String> queryParams = new LinkedHashMap<>();

        @Override public String getMethod() { return "GET"; }
        @Override public String getPath() { return "/"; }
        @Override public Map<String, String> getPathParams() { return pathParams; }
        @Override public Map<String, String> getQueryParams() { return queryParams; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public byte[] getBody() { return new byte[0]; }
        @Override public void setBody(byte[] body) { }
    }

    public static final class HttpResponseStub implements com.jvfault.web.http.HttpResponse {
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
}
