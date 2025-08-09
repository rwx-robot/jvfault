package com.jvfault.platform.servlet;

import com.jvfault.web.http.HttpRequest;
import com.jvfault.web.http.HttpResponse;
import com.jvfault.web.http.HttpContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * platform-servlet 桥接测试（Mock Servlet 对象）
 *
 * @since v0.4.0 (2018)
 */
@DisplayName("Platform-Servlet 桥接测试")
class ServletBridgeTest {

    // ============ Mock Servlet 对象 ============

    static class MockHttpServletRequest implements HttpServletRequest {
        String method = "GET";
        String uri = "/";
        final Map<String, String[]> params = new LinkedHashMap<>();
        final Map<String, String> headers = new LinkedHashMap<>();
        byte[] body = new byte[0];

        @Override public String getMethod() { return method; }
        @Override public String getRequestURI() { return uri; }
        @Override public Map<String, String[]> getParameterMap() { return params; }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return source.read(); }
                @Override public boolean isFinished() { return source.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }
        // 未使用的方法抛异常即可
        @Override public Object getAttribute(String name) { return null; }
        @Override public void setAttribute(String name, Object value) { }
        @Override public void removeAttribute(String name) { }
        @Override public String getParameter(String name) {
            String[] v = params.get(name);
            return v != null && v.length > 0 ? v[0] : null;
        }
        @Override public java.util.Enumeration<String> getHeaderNames() { return java.util.Collections.emptyEnumeration(); }
        @Override public int getContentLength() { return body.length; }
        @Override public java.util.Enumeration<String> getParameterNames() { return java.util.Collections.emptyEnumeration(); }
        // 以下为接口桩
        @Override public String getAuthType() { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.emptyEnumeration(); }
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public void setCharacterEncoding(String env) { }
        @Override public long getContentLengthLong() { return body.length; }
        @Override public String getContentType() { return null; }
        @Override public String[] getParameterValues(String name) { return params.get(name); }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 8080; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return "127.0.0.1"; }
        @Override public String getRemoteHost() { return "localhost"; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return jakarta.servlet.DispatcherType.REQUEST; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) { return null; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public int getRemotePort() { return 12345; }
        @Override public String getLocalName() { return "localhost"; }
        @Override public String getLocalAddr() { return "127.0.0.1"; }
        @Override public int getLocalPort() { return 8080; }
        @Override public String getRealPath(String path) { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() {
            return java.util.Collections.enumeration(java.util.Collections.singletonList(java.util.Locale.getDefault()));
        }

        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }

        @Override public long getDateHeader(String name) { return -1; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return java.util.Collections.emptyEnumeration(); }
        @Override public int getIntHeader(String name) { return -1; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost"); }
        @Override public String getServletPath() { return uri; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean isRequestedSessionIdFromUrl() { return false; }
        @Override public jakarta.servlet.http.Cookie[] getCookies() { return new jakarta.servlet.http.Cookie[0]; }
        @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) { }
        @Override public void logout() { }
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return java.util.Collections.emptyList(); }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public java.util.Map<String, String> getTrailerFields() { return java.util.Collections.emptyMap(); }
        @Override public boolean isTrailerFieldsReady() { return true; }
    }

    static class MockHttpServletResponse implements HttpServletResponse {
        int status = 200;
        final Map<String, String> headers = new LinkedHashMap<>();
        StringWriter body = new StringWriter();

        @Override public int getStatus() { return status; }
        @Override public void setStatus(int sc) { this.status = sc; }
        @Override public void setStatus(int sc, String sm) { this.status = sc; }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public void setContentLength(int len) { headers.put("Content-Length", String.valueOf(len)); }
        @Override public PrintWriter getWriter() { return new PrintWriter(body, true); }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() {
            return new jakarta.servlet.ServletOutputStream() {
                @Override public void write(int b) { body.write(b); }
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) { }
            };
        }
        @Override public boolean isCommitted() { return false; }
        @Override public void sendError(int sc, String msg) { status = sc; }
        @Override public void setContentType(String type) { headers.put("Content-Type", type); }
        // 其余接口桩
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) { }
        @Override public boolean containsHeader(String name) { return headers.containsKey(name); }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public String encodeUrl(String url) { return url; }
        @Override public String encodeRedirectUrl(String url) { return url; }
        @Override public void sendError(int sc) { status = sc; }
        @Override public void sendRedirect(String location) { }
        @Override public void setDateHeader(String name, long date) { }
        @Override public void addDateHeader(String name, long date) { }
        @Override public void addHeader(String name, String value) { }
        @Override public void setIntHeader(String name, int value) { }
        @Override public void addIntHeader(String name, int value) { }
        @Override public java.util.Collection<String> getHeaders(String name) { return java.util.Collections.emptyList(); }
        @Override public java.util.Collection<String> getHeaderNames() { return headers.keySet(); }
        @Override public String getContentType() { return headers.get("Content-Type"); }
        @Override public void setBufferSize(int size) { }
        @Override public int getBufferSize() { return 8192; }
        @Override public void flushBuffer() { }
        @Override public void resetBuffer() { }
        @Override public void reset() { }
        @Override public void setLocale(java.util.Locale locale) { }
        @Override public void setCharacterEncoding(String encoding) { }
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
        @Override public void setContentLengthLong(long len) { }
    }

    // ============ 桥接行为 ============

    @Test
    @DisplayName("请求桥接：方法/路径/查询参数/请求头")
    void testRequestBridge() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.method = "POST";
        servletRequest.uri = "/api/users";
        servletRequest.params.put("page", new String[]{"2"});
        servletRequest.headers.put("X-Trace", "t-1");
        servletRequest.body = "{\"name\":\"Alice\"}".getBytes(StandardCharsets.UTF_8);

        ServletRequestBridge bridge = new ServletRequestBridge(servletRequest);
        assertEquals("POST", bridge.getMethod());
        assertEquals("/api/users", bridge.getPath());
        assertEquals("2", bridge.getQueryParams().get("page"));
        assertEquals("t-1", bridge.getHeader("X-Trace"));
        assertArrayEquals(servletRequest.body, bridge.getBody());
    }

    @Test
    @DisplayName("响应桥接：缓冲刷出状态/头/体")
    void testResponseBridgeFlush() throws Exception {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletResponseBridge bridge = new ServletResponseBridge(servletResponse);

        HttpResponse view = bridge;
        view.setStatus(201);
        view.setHeader("Content-Type", "application/json");
        view.setBody("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        bridge.flush();

        assertEquals(201, servletResponse.status);
        assertEquals("application/json", servletResponse.headers.get("Content-Type"));
        assertTrue(servletResponse.body.toString().contains("ok"));
    }

    @Test
    @DisplayName("HttpContext 在桥接上工作")
    void testContextOnBridge() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.body = "{\"age\":18}".getBytes(StandardCharsets.UTF_8);
        HttpRequest req = new ServletRequestBridge(servletRequest);
        HttpResponse resp = new ServletResponseBridge(new MockHttpServletResponse());

        HttpContext context = new HttpContext(req, resp);
        context.json(200, java.util.Collections.singletonMap("hello", "world"));
        assertEquals(200, resp.getStatus());
        assertTrue(new String(resp.getBody(), StandardCharsets.UTF_8).contains("world"));
    }
}
