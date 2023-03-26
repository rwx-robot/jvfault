package com.jvfault.web.http;

/**
 * 平台无关的 HTTP 请求抽象（由 platform 模块桥接实现）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface HttpRequest {

    String getMethod();

    String getPath();

    /** 路由匹配后填充的路径参数 */
    java.util.Map<String, String> getPathParams();

    /** 查询参数（首个值） */
    java.util.Map<String, String> getQueryParams();

    String getHeader(String name);

    byte[] getBody();

    void setBody(byte[] body);
}
