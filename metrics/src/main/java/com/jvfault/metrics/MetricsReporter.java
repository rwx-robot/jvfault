package com.jvfault.metrics;

/**
 * 指标上报器 SPI。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface MetricsReporter {

    void report(MetricsSnapshot snapshot);

    /** 汇总注册表当前值的快照工厂 */
    static MetricsSnapshot snapshotOf(DefaultMeterRegistry registry) {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        for (Meter meter : registry.getMeters()) {
            String name = meter.getId().getName();
            switch (meter.getType()) {
                case COUNTER:
                    snapshot.add(name, ((Counter) meter).count());
                    break;
                case TIMER:
                    Timer timer = (Timer) meter;
                    snapshot.add(name + ".count", timer.count());
                    snapshot.add(name + ".meanMillis", timer.meanMillis());
                    snapshot.add(name + ".p99Millis", timer.percentileMillis(0.99));
                    break;
                case SUMMARY:
                    DistributionSummary summary =
                            (DistributionSummary) meter;
                    snapshot.add(name + ".count", summary.count());
                    snapshot.add(name + ".total", summary.totalAmount());
                    snapshot.add(name + ".max", summary.max());
                    break;
                case GAUGE:
                    snapshot.add(name, ((Gauge) meter).value());
                    break;
                default:
            }
        }
        return snapshot;
    }
}
