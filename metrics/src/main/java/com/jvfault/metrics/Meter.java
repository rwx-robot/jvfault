package com.jvfault.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * 指标基类与标识。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public abstract class Meter {

    private final Id id;

    private volatile boolean noop;

    protected Meter(Id id) {
        this.id = id;
    }

    /** 标记为被过滤器拒绝的空实现 */
    public void markNoop() {
        this.noop = true;
    }

    /** 被过滤器拒绝的空实现返回 true */
    public boolean isNoop() {
        return noop;
    }

    public Id getId() {
        return id;
    }

    public MeterType getType() {
        return id.getType();
    }

    /** 指标标识 */
    public static final class Id {
        private final String name;
        private final MeterType type;
        private final Map<String, String> tags;

        public Id(String name, MeterType type, Map<String, String> tags) {
            this.name = name;
            this.type = type;
            this.tags = Collections.unmodifiableMap(tags);
        }

        public String getName() {
            return name;
        }

        public MeterType getType() {
            return type;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        @Override
        public String toString() {
            return name + tags;
        }
    }
}
