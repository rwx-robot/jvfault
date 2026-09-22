package com.jvfault.ops;

/**
 * 健康检查 SPI。
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
@FunctionalInterface
public interface HealthIndicator {

    /**
     * @return 健康结果（UP/DOWN + 可选详情）
     */
    Health health();
}
