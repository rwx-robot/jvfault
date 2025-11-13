package com.jvfault.web.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 请求管道 - 按序执行 Guard → Interceptor 链 → 目标处理。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class RequestPipeline {

    private final List<Guard> guards = new ArrayList<>();
    private final List<Interceptor> interceptors = new ArrayList<>();

    public RequestPipeline addGuard(Guard guard) {
        guards.add(guard);
        return this;
    }

    public RequestPipeline addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public List<Guard> getGuards() {
        return Collections.unmodifiableList(guards);
    }

    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

    /**
     * 执行管道到目标。
     *
     * @return 目标处理结果
     * @throws com.jvfault.exception.ForbiddenException Guard 拒绝时
     */
    public Object proceed(com.jvfault.web.http.HttpContext context, Target target) throws Exception {
        for (Guard guard : guards) {
            if (!guard.canActivate(context)) {
                throw new com.jvfault.exception.ForbiddenException("被 Guard 拒绝");
            }
        }
        // 拦截器洋葱模型（先注册者在外层）
        Interceptor.Invocation leaf = target::invoke;
        Interceptor.Invocation chain = leaf;
        List<Interceptor> reversed = new ArrayList<>(interceptors);
        Collections.reverse(reversed);
        for (Interceptor interceptor : reversed) {
            Interceptor.Invocation next = chain;
            chain = ctx -> interceptor.intercept(ctx, next);
        }
        return chain.invoke(context);
    }

    /** 管道目标 */
    public interface Target {
        Object invoke(com.jvfault.web.http.HttpContext context) throws Exception;
    }
}
