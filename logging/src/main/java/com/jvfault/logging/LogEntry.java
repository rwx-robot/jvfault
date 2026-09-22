package com.jvfault.logging;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化日志条目 —— 单条日志的结构化数据模型（builder 风格，不可变）
 *
 * <p>字段说明：
 * <ul>
 *   <li>timestamp: 毫秒级时间戳（epoch millis，Builder 创建时自动填充）</li>
 *   <li>level: 日志级别（{@link LogLevel}）</li>
 *   <li>loggerName: 产生日志的 Logger 名称</li>
 *   <li>threadName: 线程名（Builder 创建时自动填充）</li>
 *   <li>message: 日志消息</li>
 *   <li>keyValuePairs: 结构化键值对（{@link LinkedHashMap}，保持插入顺序，构建后只读）</li>
 *   <li>traceId/spanId: 分布式追踪上下文，通过 {@link Builder#fromMdc()} 从 {@link MDC} 读取</li>
 * </ul>
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public final class LogEntry {

    /** MDC 中 traceId 的默认键名 */
    public static final String MDC_TRACE_ID = "traceId";

    /** MDC 中 spanId 的默认键名 */
    public static final String MDC_SPAN_ID = "spanId";

    private final long timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String threadName;
    private final String message;
    private final Map<String, Object> keyValuePairs;
    private final String traceId;
    private final String spanId;

    private LogEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.level = builder.level;
        this.loggerName = builder.loggerName;
        this.threadName = builder.threadName;
        this.message = builder.message;
        this.keyValuePairs = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.keyValuePairs));
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 结构化键值对（只读，保持插入顺序）
     */
    public Map<String, Object> getKeyValuePairs() {
        return keyValuePairs;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    @Override
    public String toString() {
        return "LogEntry{level=" + level + ", loggerName='" + loggerName + "', message='" + message
                + "', keyValuePairs=" + keyValuePairs + ", traceId='" + traceId + "', spanId='" + spanId + "'}";
    }

    /**
     * LogEntry 构建器
     *
     * <p>timestamp 与 threadName 在创建 Builder 时自动填充为当前值，可显式覆盖。
     */
    public static final class Builder {

        private long timestamp = System.currentTimeMillis();
        private LogLevel level = LogLevel.INFO;
        private String loggerName = "";
        private String threadName = Thread.currentThread().getName();
        private String message;
        private final Map<String, Object> keyValuePairs = new LinkedHashMap<String, Object>();
        private String traceId;
        private String spanId;

        private Builder() {
        }

        /** 毫秒级时间戳（epoch millis） */
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /** 日志级别 */
        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        /** Logger 名称 */
        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        /** 线程名 */
        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        /** 日志消息 */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /** 追加一个键值对（key 为空时忽略） */
        public Builder keyValue(String key, Object value) {
            if (key == null || key.isEmpty()) {
                return this;
            }
            this.keyValuePairs.put(key, value);
            return this;
        }

        /** 批量合并键值对（null 时忽略） */
        public Builder keyValuePairs(Map<String, Object> entries) {
            if (entries == null) {
                return this;
            }
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                keyValue(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /** 追踪上下文 traceId */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /** 追踪上下文 spanId */
        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        /**
         * 从 {@link MDC} 填充 traceId/spanId（键名见 {@link #MDC_TRACE_ID} / {@link #MDC_SPAN_ID}）。
         *
         * <p>仅在对应字段尚未显式设置时填充，显式设置的值优先。
         */
        public Builder fromMdc() {
            if (traceId == null) {
                String mdcTraceId = MDC.get(MDC_TRACE_ID);
                if (mdcTraceId != null) {
                    this.traceId = mdcTraceId;
                }
            }
            if (spanId == null) {
                String mdcSpanId = MDC.get(MDC_SPAN_ID);
                if (mdcSpanId != null) {
                    this.spanId = mdcSpanId;
                }
            }
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
