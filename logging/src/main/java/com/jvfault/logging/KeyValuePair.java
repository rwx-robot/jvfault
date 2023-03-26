package com.jvfault.logging;

import java.util.Objects;

/**
 * 结构化键值对 —— 结构化日志的最小数据单元
 *
 * <p>使用 {@link #of(String, Object)} 工厂方法创建，value 允许为 null。
 *
 * <p>使用示例：
 * <pre>{@code
 * logger.info("订单已创建", KeyValuePair.of("orderId", 42), KeyValuePair.of("city", "上海"));
 * }</pre>
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public final class KeyValuePair {

    private final String key;
    private final Object value;

    private KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 创建键值对
     *
     * @param key   键名，不能为空
     * @param value 值，允许为 null
     * @return 键值对实例
     */
    public static KeyValuePair of(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("KeyValuePair key 不能为空");
        }
        return new KeyValuePair(key, value);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    /**
     * 值的字符串表示（格式化器渲染时使用）
     */
    public String valueAsString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyValuePair)) {
            return false;
        }
        KeyValuePair that = (KeyValuePair) o;
        return key.equals(that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
