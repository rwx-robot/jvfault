package com.jvfault.web.http;

/**
 * 平台无关的 HTTP 响应抽象。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface HttpResponse {

    int getStatus();

    void setStatus(int status);

    String getHeader(String name);

    void setHeader(String name, String value);

    byte[] getBody();

    void setBody(byte[] body);
}
