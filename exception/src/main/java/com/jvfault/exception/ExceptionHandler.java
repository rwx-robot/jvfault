package com.jvfault.exception;

import java.lang.annotation.*;

/**
 * 标记方法为异常处理器。
 *
 * <p>处理器方法参数按类型注入（Throwable、ProblemDetail 可选），
 * 返回值支持 ProblemDetail、String（作为 detail）或 void（仅副作用）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {

    /** 处理的异常类型（含子类） */
    Class<? extends Throwable>[] value();
}
