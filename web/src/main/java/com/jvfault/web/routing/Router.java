package com.jvfault.web.routing;

import com.jvfault.exception.MethodNotAllowedException;
import com.jvfault.exception.NotFoundException;
import com.jvfault.web.http.HttpContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 路由器 - 路由表注册与查找。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class Router {

    private final List<RouteEntry> routes = new CopyOnWriteArrayList<>();

    public void add(RouteEntry entry) {
        routes.add(entry);
    }

    public int routeCount() {
        return routes.size();
    }

    public List<RouteEntry> getRoutes() {
        return new java.util.ArrayList<>(routes);
    }

    /**
     * 查找路由；命中填充路径参数。路径未命中抛 404，方法未命中抛 405。
     */
    public RouteEntry resolve(HttpContext context) {
        String method = context.getMethod();
        String path = context.getPath();

        RouteEntry best = null;
        int bestParamsFilled = -1;
        RouteEntry pathMatch = null;
        for (RouteEntry route : routes) {
            java.util.Map<String, String> params = route.getPattern().match(path);
            if (params == null) {
                continue;
            }
            pathMatch = route;
            if (route.getHttpMethod().equalsIgnoreCase(method)) {
                int specificity = route.getPattern().getSpecificity();
                if (specificity > bestParamsFilled) {
                    best = route;
                    bestParamsFilled = specificity;
                }
            }
        }
        if (best != null) {
            java.util.Map<String, String> params = best.getPattern().match(path);
            context.getRequest().getPathParams().putAll(params);
            return best;
        }
        if (pathMatch != null) {
            throw new MethodNotAllowedException(method + " 不支持 " + path);
        }
        throw new NotFoundException("无路由: " + path);
    }
}
