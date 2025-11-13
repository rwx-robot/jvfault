package com.jvfault.logging;

import java.util.Map;
import java.util.TreeMap;

/**
 * JSON 结构化日志格式化器 —— 手工转义实现，零依赖，单行输出
 *
 * <p>输出示例（单行）：
 * <pre>{@code
 * {"timestamp":1719849600123,"level":"INFO","logger":"com.demo.OrderService","thread":"main",
 *  "message":"订单已创建","traceId":"4bf9...4736","kv":{"orderId":42,"city":"上海"}}
 * }</pre>
 *
 * <p>规则：
 * <ul>
 *   <li>字符串转义覆盖 {@code \" \\ \n \r \t \b \f} 与其余控制字符（U+00XX）</li>
 *   <li>值渲染：String/其他对象 - 带引号字符串；Number/Boolean - 字面量；null - {@code null}</li>
 *   <li>traceId/spanId/kv 为空时省略对应字段</li>
 *   <li>可选按 key 字典序排序键值对（{@link #JsonLogFormatter(boolean)}）</li>
 * </ul>
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public class JsonLogFormatter implements LogFormatter {

    private final boolean sortKeys;

    /**
     * 默认构造：保持键值对插入顺序
     */
    public JsonLogFormatter() {
        this(false);
    }

    /**
     * @param sortKeys true 时按 key 字典序排序键值对
     */
    public JsonLogFormatter(boolean sortKeys) {
        this.sortKeys = sortKeys;
    }

    /**
     * 是否启用 key 排序
     */
    public boolean isSortKeys() {
        return sortKeys;
    }

    @Override
    public String format(LogEntry entry) {
        StringBuilder sb = new StringBuilder(192);
        sb.append("{\"timestamp\":").append(entry.getTimestamp());

        appendField(sb, "level", entry.getLevel() != null ? entry.getLevel().name() : null);
        appendField(sb, "logger", entry.getLoggerName());
        appendField(sb, "thread", entry.getThreadName());
        appendField(sb, "message", entry.getMessage());

        if (entry.getTraceId() != null) {
            appendField(sb, "traceId", entry.getTraceId());
        }
        if (entry.getSpanId() != null) {
            appendField(sb, "spanId", entry.getSpanId());
        }

        Map<String, Object> kvs = entry.getKeyValuePairs();
        if (!kvs.isEmpty()) {
            sb.append(",\"kv\":{");
            boolean first = true;
            for (Map.Entry<String, Object> kv : sorted(kvs).entrySet()) {
                if (kv.getKey() == null) {
                    continue; // JSON 对象键必须为字符串，忽略 null 键
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                appendString(sb, kv.getKey());
                sb.append(':');
                appendValue(sb, kv.getValue());
            }
            sb.append('}');
        }

        sb.append('}');
        return sb.toString();
    }

    private Map<String, Object> sorted(Map<String, Object> kvs) {
        if (!sortKeys) {
            return kvs;
        }
        return new TreeMap<String, Object>(kvs);
    }

    private void appendField(StringBuilder sb, String name, String value) {
        sb.append(',');
        appendString(sb, name);
        sb.append(':');
        appendString(sb, value);
    }

    private void appendString(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        escapeAndAppend(sb, s);
        sb.append('"');
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
            return;
        }
        appendString(sb, String.valueOf(value));
    }

    /**
     * 手工 JSON 字符串转义：\" \\ \n \r \t \b \f 与控制字符（&lt; 0x20, U+00XX）
     */
    static void escapeAndAppend(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append('\\').append('"');
                    break;
                case '\\':
                    sb.append('\\').append('\\');
                    break;
                case '\n':
                    sb.append('\\').append('n');
                    break;
                case '\r':
                    sb.append('\\').append('r');
                    break;
                case '\t':
                    sb.append('\\').append('t');
                    break;
                case '\b':
                    sb.append('\\').append('b');
                    break;
                case '\f':
                    sb.append('\\').append('f');
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
    }
}
