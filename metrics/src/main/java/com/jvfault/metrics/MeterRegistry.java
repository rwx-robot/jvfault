package com.jvfault.metrics;

import java.util.Map;

/**
 * 指标注册表接口。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface MeterRegistry {

    Counter counter(String name, String... tags);

    Timer timer(String name, String... tags);

    DistributionSummary summary(String name, String... tags);

    Gauge gauge(String name, Number number, String... tags);

    Gauge gauge(String name, java.util.function.DoubleSupplier supplier, String... tags);

    void setFilter(MeterFilter filter);

    Meter find(String name, String... tags);

    void clear();
}
