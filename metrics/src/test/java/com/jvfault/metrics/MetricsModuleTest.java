package com.jvfault.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-metrics 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("Metrics 模块测试")
class MetricsModuleTest {

    @Test
    @DisplayName("计数器聚合")
    void testCounter() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        Counter counter = registry.counter("requests.total", "method", "GET");
        counter.increment();
        counter.increment(4);
        assertEquals(5, counter.count(), 0.001);

        // 同 id 复用
        assertSame(counter, registry.counter("requests.total", "method", "GET"));
    }

    @Test
    @DisplayName("tag 顺序无关")
    void testTagOrderIndependent() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        Counter a = registry.counter("api", "method", "GET", "status", "200");
        Counter b = registry.counter("api", "status", "200", "method", "GET");
        assertSame(a, b);
    }

    @Test
    @DisplayName("Timer 统计与百分位")
    void testTimer() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        Timer timer = registry.timer("job.duration");
        timer.record(1_000_000);   // 1ms
        timer.record(3_000_000);   // 3ms
        timer.record(10_000_000);  // 10ms

        assertEquals(3, timer.count());
        assertEquals(14.0 / 3, timer.meanMillis(), 0.001);
        assertEquals(10, timer.maxMillis());
        assertTrue(timer.percentileMillis(1.0) >= 10);
        assertEquals(0, registry.timer("empty").count());
    }

    @Test
    @DisplayName("Gauge 读取实时值")
    void testGauge() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        java.util.concurrent.atomic.AtomicInteger backing = new java.util.concurrent.atomic.AtomicInteger(42);
        Gauge gauge = registry.gauge("queue.size", () -> backing.get());
        assertEquals(42, gauge.value(), 0.001);
        backing.set(100);
        assertEquals(100, gauge.value(), 0.001);
    }

    @Test
    @DisplayName("DistributionSummary")
    void testSummary() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        DistributionSummary summary = registry.summary("payload.size");
        summary.record(100);
        summary.record(300);
        assertEquals(2, summary.count());
        assertEquals(400, summary.totalAmount(), 0.001);
        assertEquals(300, summary.max());
    }

    @Test
    @DisplayName("MeterFilter 拒绝前缀")
    void testFilter() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        registry.setFilter(MeterFilter.denyNamePrefix("internal."));
        Counter denied = registry.counter("internal.secret");
        assertTrue(denied.isNoop());
        denied.increment(999);
        assertEquals(0, denied.count(), 0.001);
    }

    @Test
    @DisplayName("并发计数正确")
    void testConcurrentIncrement() throws Exception {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        Counter counter = registry.counter("concurrent");
        int threads = 8;
        int perThread = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    counter.increment();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(threads * perThread, counter.count(), 0.001);
    }

    @Test
    @DisplayName("快照与 JSON 导出")
    void testSnapshot() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        registry.counter("hits").increment(3);
        registry.gauge("temp", 36.6);

        MetricsSnapshot snapshot = MetricsReporter.snapshotOf(registry);
        String json = snapshot.toJson();
        assertTrue(json.contains("\"hits\":3"));
        assertTrue(json.contains("\"temp\":36.6"));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    @DisplayName("find 与 clear")
    void testFindAndClear() {
        DefaultMeterRegistry registry = new DefaultMeterRegistry();
        registry.counter("find.me");
        assertNotNull(registry.find("find.me"));
        registry.clear();
        assertNull(registry.find("find.me"));
    }
}
