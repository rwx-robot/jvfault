package com.jvfault.platform.servlet;

import com.jvfault.web.http.HttpResponse;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HttpResponse -> HttpServletResponse 桥接（缓冲模式：WebApplication
 * 分发完成后统一刷出）。
 *
 * @since v0.4.0 (2018)
 * @author jvfault team
 */
public class ServletResponseBridge implements HttpResponse {

    private final HttpServletResponse delegate;
    private int status = 200;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public ServletResponseBridge(HttpServletResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * 把缓冲的响应刷到 Servlet 容器。
     */
    public void flush() throws IOException {
        delegate.setStatus(status);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            delegate.setHeader(e.getKey(), e.getValue());
        }
        if (body.length > 0) {
            delegate.setContentLength(body.length);
            delegate.getOutputStream().write(body);
        }
    }
}
