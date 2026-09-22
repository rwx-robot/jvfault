package com.jvfault.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异常建议 - 统一入口：解析处理器并产出 ProblemDetail。
 *
 * <p>无匹配处理器时：HttpException 用其自带 ProblemDetail，
 * 其余异常统一 500。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

    private final ExceptionHandlerRegistry registry;
    private final boolean includeStackTrace;

    public ExceptionAdvice(ExceptionHandlerRegistry registry) {
        this(registry, false);
    }

    public ExceptionAdvice(ExceptionHandlerRegistry registry, boolean includeStackTrace) {
        this.registry = registry;
        this.includeStackTrace = includeStackTrace;
    }

    /**
     * 处理异常，返回标准化 ProblemDetail。
     */
    public ProblemDetail handle(Throwable throwable) {
        return registry.resolve(throwable)
                .map(handler -> {
                    try {
                        ProblemDetail fallback = ProblemDetail.of(throwable);
                        Object result = handler.invoke(throwable, fallback);
                        return toProblemDetail(result, fallback);
                    } catch (Throwable handlerError) {
                        log.error("Exception handler {} failed while handling {}",
                                handler.getMethod(), throwable.getClass().getSimpleName(), handlerError);
                        return fallbackProblem(throwable);
                    }
                })
                .orElseGet(() -> fallbackProblem(throwable));
    }

    private ProblemDetail fallbackProblem(Throwable throwable) {
        ProblemDetail pd = ProblemDetail.of(throwable);
        if (includeStackTrace) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement e : throwable.getStackTrace()) {
                sb.append("\n\tat ").append(e);
            }
            pd.property("stacktrace", sb.toString());
        }
        return pd;
    }

    private ProblemDetail toProblemDetail(Object result, ProblemDetail fallback) {
        if (result instanceof ProblemDetail) {
            ProblemDetail pd = (ProblemDetail) result;
            if (pd.getStatus() <= 0) {
                pd.setStatus(fallback.getStatus());
            }
            return pd;
        }
        if (result instanceof String) {
            ProblemDetail copy = ProblemDetail.forStatusAndDetail(
                    fallback.getStatus(), (String) result);
            copy.setTitle(fallback.getTitle());
            return copy;
        }
        return fallback;
    }
}
