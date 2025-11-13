package com.jvfault.tracing;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * W3C Trace Context 传播器 —— 处理 "traceparent"/"tracestate" 头的注入与提取
 * 对应 OpenTelemetry: W3CTraceContextPropagator
 *
 * <p>traceparent 格式：{@code 00-<traceId 32 hex>-<parentSpanId 16 hex>-<traceFlags 2 hex>}。
 * 仅支持 version=00；tracestate 头原样透传。
 *
 * <p>容错规则：任何非法输入一律忽略 —— {@link #extract} 返回 null，
 * 不抛出异常，保证业务链路不受脏头影响。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public final class W3CPropagation {

    /** traceparent 头名称 */
    public static final String TRACEPARENT = "traceparent";

    /** tracestate 头名称 */
    public static final String TRACESTATE = "tracestate";

    private static final String SUPPORTED_VERSION = "00";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern SPAN_ID_PATTERN = Pattern.compile("^[0-9a-f]{16}$");
    private static final Pattern FLAGS_PATTERN = Pattern.compile("^[0-9a-f]{2}$");

    private W3CPropagation() {
    }

    // ============ 注入 ============

    /**
     * 将追踪上下文注入到 Map 载体（如 HTTP 头）
     */
    public static void inject(TraceContext context, Map<String, String> carrier) {
        if (carrier == null) {
            throw new IllegalArgumentException("carrier 不能为空");
        }
        inject(context, new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                carrier.put(key, value);
            }
        });
    }

    /**
     * 将追踪上下文通过 setter 回调注入载体
     *
     * @param context 追踪上下文（不能为 null）
     * @param setter  (key, value) 写入回调
     */
    public static void inject(TraceContext context, BiConsumer<String, String> setter) {
        if (context == null) {
            throw new IllegalArgumentException("context 不能为空");
        }
        if (setter == null) {
            throw new IllegalArgumentException("setter 不能为空");
        }
        setter.accept(TRACEPARENT, format(context));
        if (context.getTraceState() != null && !context.getTraceState().isEmpty()) {
            setter.accept(TRACESTATE, context.getTraceState());
        }
    }

    /**
     * 渲染 traceparent 头：{@code 00-<traceId>-<spanId>-<flags>}
     */
    public static String format(TraceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context 不能为空");
        }
        return SUPPORTED_VERSION + "-" + context.getTraceId() + "-" + context.getSpanId()
                + "-" + twoHexDigits(context.getTraceFlags());
    }

    // ============ 提取 ============

    /**
     * 从 Map 载体提取追踪上下文（载体为 null 或无有效头时返回 null）
     *
     * <p>traceparent 中的 parent-id 字段被解析为返回上下文的 spanId（远端当前 Span）。
     */
    public static TraceContext extract(Map<String, String> carrier) {
        if (carrier == null) {
            return null;
        }
        return extract(new Function<String, String>() {
            @Override
            public String apply(String key) {
                return carrier.get(key);
            }
        });
    }

    /**
     * 通过 getter 回调提取追踪上下文
     *
     * @param getter 头名称 -&gt; 值 的读取回调
     * @return 提取的上下文；头缺失或格式非法时返回 null
     */
    public static TraceContext extract(Function<String, String> getter) {
        if (getter == null) {
            return null;
        }
        String traceparent = getter.apply(TRACEPARENT);
        if (traceparent == null) {
            return null;
        }
        traceparent = traceparent.trim();

        String[] parts = traceparent.split("-", -1);
        if (parts.length != 4 || !SUPPORTED_VERSION.equals(parts[0])) {
            return null;
        }
        String traceId = parts[1];
        String parentSpanId = parts[2];
        String flags = parts[3];

        if (!TRACE_ID_PATTERN.matcher(traceId).matches() || TraceContext.isAllZero(traceId)) {
            return null;
        }
        if (!SPAN_ID_PATTERN.matcher(parentSpanId).matches() || TraceContext.isAllZero(parentSpanId)) {
            return null;
        }
        if (!FLAGS_PATTERN.matcher(flags).matches()) {
            return null;
        }
        int flagsValue = Integer.parseInt(flags, 16);

        String tracestate = getter.apply(TRACESTATE);
        if (tracestate != null && tracestate.isEmpty()) {
            tracestate = null;
        }
        return new TraceContext(traceId, null, parentSpanId, flagsValue, tracestate);
    }

    // ============ 内部实现 ============

    private static String twoHexDigits(int flags) {
        String hex = Integer.toHexString(flags & 0xFF);
        return hex.length() < 2 ? "0" + hex : hex;
    }
}
