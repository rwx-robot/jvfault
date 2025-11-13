package com.jvfault.ops;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康聚合器 —— 汇总全部 HealthIndicator（任一 DOWN 即 DOWN）。
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public class HealthAggregator {

    private final Map<String, HealthIndicator> indicators = new LinkedHashMap<>();

    public HealthAggregator register(String name, HealthIndicator indicator) {
        indicators.put(name, indicator);
        return this;
    }

    /** K8s liveness 语义：进程存活即 UP */
    public Health liveness() {
        return Health.up(java.util.Collections.singletonMap("mode", "liveness"));
    }

    /** K8s readiness 语义：全部指标 UP 才就绪 */
    public Health readiness() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allUp = true;
        for (Map.Entry<String, HealthIndicator> e : indicators.entrySet()) {
            Health health = e.getValue().health();
            details.put(e.getKey(), health.getStatus().name());
            allUp &= health.isUp();
        }
        return allUp ? Health.up(details) : Health.down(details);
    }

    public Map<String, HealthIndicator> getIndicators() {
        return new LinkedHashMap<>(indicators);
    }
}
