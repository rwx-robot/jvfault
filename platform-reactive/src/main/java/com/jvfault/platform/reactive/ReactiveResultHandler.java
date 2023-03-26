package com.jvfault.platform.reactive;

import com.jvfault.web.http.HttpContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 响应式结果渲染器 - 把 Mono/Flux 处理器结果渲染到 HttpContext。
 *
 * <p>语义：
 * <ul>
 *   <li>Mono&lt;T&gt;：完成时渲染（JSON 对象或文本），空 Mono 渲染 204</li>
 *   <li>Flux&lt;T&gt;：按 NDJSON（逐行 JSON）渲染 200</li>
 *   <li>错误信号：交由全局异常处理（RuntimeException 原样抛出）</li>
 * </ul>
 *
 * @since v0.8.0 (2022)
 * @author jvfault team
 */
public class ReactiveResultHandler {

    public void render(Object result, HttpContext context) {
        if (result instanceof Mono) {
            ((Mono<?>) result).doOnNext(value -> writeOne(value, context))
                    .switchIfEmpty(Mono.fromRunnable(() -> context.getResponse().setStatus(204)))
                    .block();
        } else if (result instanceof Flux) {
            StringBuilder ndjson = new StringBuilder();
            ((Flux<?>) result).doOnNext(value -> ndjson.append(toJson(value)).append('\n'))
                    .blockLast();
            context.getResponse().setStatus(200);
            context.getResponse().setHeader("Content-Type", "application/x-ndjson");
            context.getResponse().setBody(ndjson.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else {
            throw new IllegalArgumentException("非响应式结果: " + (result == null ? "null" : result.getClass()));
        }
    }

    private void writeOne(Object value, HttpContext context) {
        context.getResponse().setStatus(200);
        context.getResponse().setHeader("Content-Type", "application/json; charset=utf-8");
        context.getResponse().setBody(toJson(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String toJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }
}
