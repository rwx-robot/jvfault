package com.jvfault.web.routing;

import com.jvfault.web.http.HttpContext;

import java.lang.reflect.Method;

/**
 * 路由表项：HTTP 方法 + 模式 + 控制器方法。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class RouteEntry {

    private final String httpMethod;
    private final RoutePattern pattern;
    private final Object controller;
    private final Method handler;

    public RouteEntry(String httpMethod, RoutePattern pattern, Object controller, Method handler) {
        this.httpMethod = httpMethod;
        this.pattern = pattern;
        this.controller = controller;
        this.handler = handler;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public RoutePattern getPattern() {
        return pattern;
    }

    public Object getController() {
        return controller;
    }

    public Method getHandler() {
        return handler;
    }

    /**
     * 匹配请求（方法 + 路径），命中返回填充了路径参数的上下文。
     */
    public HttpContext match(String method, String path, HttpContext context) {
        if (!httpMethod.equalsIgnoreCase(method)) {
            return null;
        }
        java.util.Map<String, String> params = pattern.match(path);
        if (params == null) {
            return null;
        }
        context.getRequest().getPathParams().putAll(params);
        return context;
    }
}
