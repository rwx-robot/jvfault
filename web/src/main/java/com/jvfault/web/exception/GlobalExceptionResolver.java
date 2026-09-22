package com.jvfault.web.exception;

import com.jvfault.exception.ExceptionAdvice;
import com.jvfault.exception.ExceptionHandlerRegistry;
import com.jvfault.exception.ProblemDetail;
import com.jvfault.web.http.HttpContext;

/**
 * Web 层全局异常解析 - 基于 exception 模块的注册表与建议器。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class GlobalExceptionResolver {

    private final ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();
    private final ExceptionAdvice advice = new ExceptionAdvice(registry);

    /**
     * 注册用户异常处理器 Bean（扫描 @ExceptionHandler 方法）。
     */
    public void registerHandler(Object handlerBean) {
        registry.register(handlerBean);
    }

    /**
     * 解析异常为 ProblemDetail。
     */
    public ProblemDetail resolve(HttpContext context, Throwable throwable) {
        return advice.handle(throwable);
    }
}
