package com.jvfault.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指标快照 - 全部指标的瞬时值导出。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class MetricsSnapshot {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public MetricsSnapshot add(String name, Object value) {
        values.put(name, value);
        return this;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * 手工 JSON 序列化（无 jackson 依赖）。
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(quote(e.getKey())).append(':');
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append(quote(String.valueOf(v)));
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                default:   sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
