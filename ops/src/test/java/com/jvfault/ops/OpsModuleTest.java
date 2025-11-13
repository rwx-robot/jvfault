package com.jvfault.ops;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-ops 核心测试
 *
 * @since v1.0.0 (2026)
 */
@DisplayName("Ops 模块测试")
class OpsModuleTest {

    @Test
    @DisplayName("Health 聚合: 任一 DOWN 即 DOWN")
    void testReadinessAggregation() {
        HealthAggregator aggregator = new HealthAggregator()
                .register("db", () -> Health.up(new java.util.LinkedHashMap<String, Object>() {{ put("latency", 5); }}))
                .register("cache", Health::down);

        Health readiness = aggregator.readiness();
        assertFalse(readiness.isUp());
        assertEquals("UP", readiness.getDetails().get("db"));
        assertEquals("DOWN", readiness.getDetails().get("cache"));

        // 修复 cache 后就绪
        HealthAggregator healthy = new HealthAggregator()
                .register("db", Health::up)
                .register("cache", Health::up);
        assertTrue(healthy.readiness().isUp());
    }

    @Test
    @DisplayName("liveness 恒为 UP")
    void testLiveness() {
        assertTrue(new HealthAggregator().liveness().isUp());
        assertEquals("liveness", new HealthAggregator().liveness().getDetails().get("mode"));
    }

    @Test
    @DisplayName("GracefulShutdown 按序执行钩子")
    void testGracefulShutdownHooks() {
        GracefulShutdown shutdown = new GracefulShutdown();
        List<String> order = new java.util.ArrayList<>();
        shutdown.addHook(() -> order.add("stop-server"));
        shutdown.addHook(() -> order.add("close-db"));

        assertEquals(2, shutdown.hookCount());
        assertFalse(shutdown.isShuttingDown());

        // 手动触发（等价 JVM hook 路径的钩子集合）
        for (Runnable hook : shutdown.getHooks()) {
            hook.run();
        }
        assertEquals(java.util.Arrays.asList("stop-server", "close-db"), order);
    }

    @Test
    @DisplayName("钩子异常不中断后续钩子")
    void testHookFailureIsolation() {
        GracefulShutdown shutdown = new GracefulShutdown();
        AtomicBoolean secondRan = new AtomicBoolean(false);
        shutdown.addHook(() -> {
            throw new RuntimeException("boom");
        });
        shutdown.addHook(() -> secondRan.set(true));

        // install 后的 hook 路径捕获异常
        shutdown.install();
        for (Runnable hook : shutdown.getHooks()) {
            try {
                hook.run();
            } catch (RuntimeException ignored) {
            }
        }
        assertTrue(secondRan.get());
    }
}
