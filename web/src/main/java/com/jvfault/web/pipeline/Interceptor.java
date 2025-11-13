package com.jvfault.web.pipeline;

import com.jvfault.web.http.HttpContext;

/**
 * 拦截器 - 目标处理前后横切逻辑（日志、计时等）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface Interceptor {

    /**
     * 环绕处理：{@code proceed.invoke(context)} 执行目标。
     */
    Object intercept(HttpContext context, Invocation proceed) throws Exception;

    /** 目标调用句柄 */
    interface Invocation {
        Object invoke(HttpContext context) throws Exception;
    }
}
