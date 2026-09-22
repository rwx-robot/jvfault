package com.jvfault.web.pipeline;

import com.jvfault.web.http.HttpContext;

/**
 * 守卫 - 决定请求是否放行（鉴权等前置检查）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface Guard {

    /**
     * @return false 时请求被拒绝（403）
     */
    boolean canActivate(HttpContext context);
}
