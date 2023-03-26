package com.jvfault.metrics;

/**
 * 指标过滤器。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface MeterFilter {

    boolean accepts(Meter.Id id);

    static MeterFilter allowAll() {
        return id -> true;
    }

    static MeterFilter denyNamePrefix(String prefix) {
        return id -> !id.getName().startsWith(prefix);
    }

    static MeterFilter denyAll() {
        return id -> false;
    }
}
