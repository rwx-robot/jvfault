package com.jvfault.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP 交换上下文 - 平台无关的请求/响应封装。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class HttpContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpRequest request;
    private final HttpResponse response;

    public HttpContext(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getPath() {
        return request.getPath();
    }

    public String getParam(String name) {
        return request.getPathParams().get(name);
    }

    public String getQuery(String name) {
        return request.getQueryParams().get(name);
    }

    // ============ 响应辅助 ============

    public void json(int status, Object body) {
        try {
            response.setStatus(status);
            response.setHeader("Content-Type", "application/json; charset=utf-8");
            response.setBody(MAPPER.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    public void json(Object body) {
        json(200, body);
    }

    public void text(int status, String body) {
        response.setStatus(status);
        response.setHeader("Content-Type", "text/plain; charset=utf-8");
        response.setBody(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 请求体反序列化。
     */
    public <T> T readBody(Class<T> type) {
        byte[] body = request.getBody();
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            return MAPPER.readValue(body, type);
        } catch (Exception e) {
            throw new com.jvfault.exception.BadRequestException("请求体不是合法 JSON: " + type.getSimpleName());
        }
    }
}
