package com.jvfault.tracing;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 追踪上下文 —— W3C Trace Context 的不可变数据模型
 * 对应 OpenTelemetry: SpanContext
 *
 * <p>字段遵循 W3C Trace Context 规范：
 * <ul>
 *   <li>traceId: 32 位小写十六进制（16 字节，非全零）</li>
 *   <li>parentId: 16 位小写十六进制（8 字节），root Span 时为 null</li>
 *   <li>spanId: 16 位小写十六进制（8 字节，非全零）</li>
 *   <li>traceFlags: 标志位（bit 0 = sampled 采样位）</li>
 *   <li>traceState: 厂商扩展字符串，可空</li>
 * </ul>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public final class TraceContext {

    /** W3C sampled 采样标志位（traceFlags bit 0） */
    public static final int FLAG_SAMPLED = 0x01;

    private static final Pattern HEX_32 = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern HEX_16 = Pattern.compile("^[0-9a-f]{16}$");

    private final String traceId;
    private final String parentId;
    private final String spanId;
    private final int traceFlags;
    private final String traceState;

    /**
     * 创建追踪上下文（参数非法时抛出 IllegalArgumentException）
     *
     * @param traceId    32 位小写十六进制，非全零
     * @param parentId   16 位小写十六进制，root Span 时为 null
     * @param spanId     16 位小写十六进制，非全零
     * @param traceFlags 标志位（0x00-0xFF）
     * @param traceState 厂商扩展字符串，可空
     */
    public TraceContext(String traceId, String parentId, String spanId, int traceFlags, String traceState) {
        if (traceId == null || !HEX_32.matcher(traceId).matches() || isAllZero(traceId)) {
            throw new IllegalArgumentException("traceId 必须为 32 位非全零小写十六进制: " + traceId);
        }
        if (spanId == null || !HEX_16.matcher(spanId).matches() || isAllZero(spanId)) {
            throw new IllegalArgumentException("spanId 必须为 16 位非全零小写十六进制: " + spanId);
        }
        if (parentId != null && !HEX_16.matcher(parentId).matches()) {
            throw new IllegalArgumentException("parentId 必须为 16 位小写十六进制或 null: " + parentId);
        }
        if (traceFlags < 0 || traceFlags > 0xFF) {
            throw new IllegalArgumentException("traceFlags 必须在 0x00-0xFF 之间: " + traceFlags);
        }
        this.traceId = traceId;
        this.parentId = parentId;
        this.spanId = spanId;
        this.traceFlags = traceFlags;
        this.traceState = traceState;
    }

    /**
     * 便捷工厂：默认采样（traceFlags = FLAG_SAMPLED），无 traceState
     */
    public static TraceContext create(String traceId, String parentId, String spanId) {
        return new TraceContext(traceId, parentId, spanId, FLAG_SAMPLED, null);
    }

    public String getTraceId() {
        return traceId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getSpanId() {
        return spanId;
    }

    public int getTraceFlags() {
        return traceFlags;
    }

    public String getTraceState() {
        return traceState;
    }

    /**
     * 是否采样（W3C traceFlags bit 0）
     */
    public boolean isSampled() {
        return (traceFlags & FLAG_SAMPLED) != 0;
    }

    static boolean isAllZero(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TraceContext)) {
            return false;
        }
        TraceContext that = (TraceContext) o;
        return traceFlags == that.traceFlags
                && traceId.equals(that.traceId)
                && Objects.equals(parentId, that.parentId)
                && spanId.equals(that.spanId)
                && Objects.equals(traceState, that.traceState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, parentId, spanId, traceFlags, traceState);
    }

    @Override
    public String toString() {
        return "TraceContext{traceId='" + traceId + "', parentId='" + parentId + "', spanId='" + spanId
                + "', traceFlags=0x" + String.format("%02x", traceFlags) + ", traceState='" + traceState + "'}";
    }
}
