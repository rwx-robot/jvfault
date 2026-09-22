package com.jvfault.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 人类可读的纯文本格式化器（单行）
 *
 * <p>输出示例（单行）：
 * <pre>{@code
 * 2024-07-01 12:00:00.123+08:00 [INFO] [com.demo.OrderService] [main] 订单已创建 {orderId=42, city=上海} [traceId=4bf9..., spanId=00f0...]
 * }</pre>
 *
 * <p>键值对与追踪上下文为空时省略对应段落。
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public class PlainTextFormatter implements LogFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXXX");

    @Override
    public String format(LogEntry entry) {
        StringBuilder sb = new StringBuilder(160);

        sb.append(Instant.ofEpochMilli(entry.getTimestamp()).atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT));
        sb.append(" [").append(entry.getLevel() != null ? entry.getLevel().name() : "null").append(']');
        sb.append(" [").append(entry.getLoggerName()).append(']');
        sb.append(" [").append(entry.getThreadName()).append(']');
        // 消息段两侧保留空格，保证 " msg " 可被稳定匹配
        sb.append(' ').append(entry.getMessage()).append(' ');

        Map<String, Object> kvs = entry.getKeyValuePairs();
        boolean hasKvs = !kvs.isEmpty();
        boolean hasTrace = entry.getTraceId() != null || entry.getSpanId() != null;

        if (hasKvs) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> kv : kvs.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(kv.getKey()).append('=').append(String.valueOf(kv.getValue()));
            }
            sb.append('}');
            if (hasTrace) {
                sb.append(' ');
            }
        }

        if (hasTrace) {
            sb.append(" [");
            boolean first = true;
            if (entry.getTraceId() != null) {
                sb.append("traceId=").append(entry.getTraceId());
                first = false;
            }
            if (entry.getSpanId() != null) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("spanId=").append(entry.getSpanId());
            }
            sb.append(']');
        }

        return sb.toString();
    }
}
