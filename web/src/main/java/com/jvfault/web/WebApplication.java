package com.jvfault.web;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.annotation.Component;
import com.jvfault.web.annotation.*;
import com.jvfault.web.exception.GlobalExceptionResolver;
import com.jvfault.web.http.HttpContext;
import com.jvfault.web.pipeline.Guard;
import com.jvfault.web.pipeline.Interceptor;
import com.jvfault.web.pipeline.RequestPipeline;
import com.jvfault.web.routing.RouteEntry;
import com.jvfault.web.routing.RoutePattern;
import com.jvfault.web.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Web 应用 - 从容器收集控制器、构建路由、驱动请求分发。
 *
 * <p>使用（容器启动后）：
 * <pre>{@code
 * ModuleContainer container = JvfaultApplication.run(AppModule.class);
 * WebApplication app = WebApplication.of(container);
 * app.addGuard(...); app.addInterceptor(...);
 * app.dispatch(httpContext); // platform 模块桥接 HTTP
 * }</pre>
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class WebApplication {

    private static final Logger log = LoggerFactory.getLogger(WebApplication.class);

    private final BeanRegistry registry;
    private final Router router = new Router();
    private final RequestPipeline pipeline = new RequestPipeline();
    private final HandlerInvoker invoker = new HandlerInvoker();
    private final GlobalExceptionResolver exceptionResolver = new GlobalExceptionResolver();
    private final List<Object> controllers = new ArrayList<>();

    public WebApplication(BeanRegistry registry) {
        this.registry = registry;
    }

    /**
     * 从容器收集全部 @Controller Bean 并注册路由。
     */
    public static WebApplication of(com.jvfault.core.module.ModuleContainer container) {
        WebApplication app = new WebApplication(container.getBeanRegistry());
        java.util.Map<String, Object> controllerBeans =
                container.getBeanRegistry().getBeansWithAnnotation(Controller.class);
        for (Object controllerBean : controllerBeans.values()) {
            app.registerController(controllerBean);
        }
        log.info("WebApplication ready: {} controllers, {} routes",
                app.controllers.size(), app.getRouter().routeCount());
        return app;
    }

    /**
     * 注册单个控制器 Bean 的全部路由。
     */
    public void registerController(Object controllerBean) {
        Class<?> clazz = controllerBean.getClass();
        Controller controllerAnn = clazz.getAnnotation(Controller.class);
        String prefix = controllerAnn != null ? controllerAnn.value() : "";

        for (Method method : controllerBean.getClass().getDeclaredMethods()) {
            String verb = httpVerbOf(method);
            if (verb == null) {
                continue;
            }
            String path = pathOf(method);
            String fullPath = joinPath(prefix, path);
            router.add(new RouteEntry(verb, new RoutePattern(fullPath), controllerBean, method));
        }
        controllers.add(controllerBean);
    }

    /** 直接检查各动词注解（注解元注解不参与运行时反射） */
    private String httpVerbOf(Method method) {
        if (method.isAnnotationPresent(Get.class)) return "GET";
        if (method.isAnnotationPresent(Post.class)) return "POST";
        if (method.isAnnotationPresent(Put.class)) return "PUT";
        if (method.isAnnotationPresent(Delete.class)) return "DELETE";
        if (method.isAnnotationPresent(Patch.class)) return "PATCH";
        return null;
    }

    private String pathOf(Method method) {
        Get get = method.getAnnotation(Get.class);
        if (get != null) return get.value();
        Post post = method.getAnnotation(Post.class);
        if (post != null) return post.value();
        Put put = method.getAnnotation(Put.class);
        if (put != null) return put.value();
        Delete delete = method.getAnnotation(Delete.class);
        if (delete != null) return delete.value();
        Patch patch = method.getAnnotation(Patch.class);
        if (patch != null) return patch.value();
        return "";
    }

    private static String joinPath(String prefix, String path) {
        String p = path.isEmpty() ? "" : path;
        if (prefix.isEmpty()) {
            return p.startsWith("/") ? p : "/" + p;
        }
        String base = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String rest = p.startsWith("/") ? p : "/" + p;
        return base + rest;
    }

    // ============ 管道装配 ============

    public WebApplication addGuard(Guard guard) {
        pipeline.addGuard(guard);
        return this;
    }

    public WebApplication addInterceptor(Interceptor interceptor) {
        pipeline.addInterceptor(interceptor);
        return this;
    }

    public GlobalExceptionResolver getExceptionResolver() {
        return exceptionResolver;
    }

    public Router getRouter() {
        return router;
    }

    public List<Object> getControllers() {
        return controllers;
    }

    // ============ 请求分发 ============

    /**
     * 分发请求：路由解析 → 管道 → 参数绑定 → 处理 → 响应渲染。
     */
    public void dispatch(HttpContext context) {
        try {
            RouteEntry route = router.resolve(context);
            pipeline.proceed(context, ctx -> {
                Object result = invoker.invoke(ctx, route.getController(), route.getHandler());
                render(ctx, result);
                return result;
            });
        } catch (Throwable throwable) {
            handleException(context, throwable);
        }
    }

    private void render(HttpContext context, Object result) {
        if (result == null || context.getResponse().getBody().length > 0) {
            return; // 处理器已自行写出响应
        }
        if (result instanceof String) {
            context.text(200, (String) result);
        } else {
            context.json(200, result);
        }
    }

    private void handleException(HttpContext context, Throwable throwable) {
        com.jvfault.exception.ProblemDetail problem =
                exceptionResolver.resolve(context, throwable);
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("type", problem.getType());
        map.put("title", problem.getTitle());
        map.put("status", problem.getStatus());
        if (problem.getDetail() != null) {
            map.put("detail", problem.getDetail());
        }
        map.putAll(problem.getProperties());
        context.json(problem.getStatus(), map);
    }
}
