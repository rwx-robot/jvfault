package com.jvfault.ops;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康结果。
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public final class Health {

    private final Status status;
    private final Map<String, Object> details;

    public enum Status { UP, DOWN }

    private Health(Status status, Map<String, Object> details) {
        this.status = status;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static Health up() {
        return new Health(Status.UP, java.util.Collections.emptyMap());
    }

    public static Health up(Map<String, Object> details) {
        return new Health(Status.UP, details);
    }

    public static Health down() {
        return new Health(Status.DOWN, java.util.Collections.emptyMap());
    }

    public static Health down(Map<String, Object> details) {
        return new Health(Status.DOWN, details);
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public boolean isUp() {
        return status == Status.UP;
    }
}
