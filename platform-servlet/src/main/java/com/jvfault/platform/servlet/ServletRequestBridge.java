package com.jvfault.platform.servlet;

import com.jvfault.web.http.HttpRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HttpServletRequest -> HttpRequest 桥接。
 *
 * @since v0.4.0 (2018)
 * @author jvfault team
 */
public class ServletRequestBridge implements HttpRequest {

    private final HttpServletRequest delegate;
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private final Map<String, String> pathParams = new LinkedHashMap<>();
    private byte[] body;

    public ServletRequestBridge(HttpServletRequest delegate) {
        this.delegate = delegate;
        delegate.getParameterMap().forEach((name, values) -> {
            if (values != null && values.length > 0) {
                queryParams.put(name, values[0]);
            }
        });
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getPath() {
        return delegate.getRequestURI();
    }

    @Override
    public Map<String, String> getPathParams() {
        return pathParams;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public String getHeader(String name) {
        return delegate.getHeader(name);
    }

    @Override
    public synchronized byte[] getBody() {
        if (body == null) {
            try (InputStream in = delegate.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
                body = out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("读取请求体失败", e);
            }
        }
        return body;
    }

    @Override
    public void setBody(byte[] body) {
        this.body = body;
    }

    public HttpServletRequest getDelegate() {
        return delegate;
    }

    /** 供测试使用 */
    public Map<String, String> headersView() {
        return Collections.emptyMap();
    }
}
