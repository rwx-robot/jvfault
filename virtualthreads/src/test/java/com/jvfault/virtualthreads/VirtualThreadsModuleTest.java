package com.jvfault.virtualthreads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-virtualthreads 核心测试
 *
 * @since v0.8.0 (2022)
 */
@DisplayName("VirtualThreads 模块测试")
class VirtualThreadsModuleTest {

    @Test
    @DisplayName("任务在虚拟线程上执行")
    void testRunsOnVirtualThreads() throws Exception {
        try (ExecutorService executor = VirtualThreadExecutors.newPerTaskExecutor()) {
            Set<Boolean> virtualFlags = ConcurrentHashMap.newKeySet();
            for (int i = 0; i < 100; i++) {
                executor.submit(() -> {
                    virtualFlags.add(Thread.currentThread().isVirtual());
                    return null;
                });
            }
            executor.awaitTermination(2, TimeUnit.SECONDS);
            assertEquals(Set.of(Boolean.TRUE), virtualFlags, "任务应全部跑在虚拟线程上");
        }
    }

    @Test
    @DisplayName("有界执行器：并发不超过上限")
    void testBoundedExecutor() throws Exception {
        int maxConcurrent = 4;
        ExecutorService executor = VirtualThreadExecutors.newBoundedExecutor(maxConcurrent);
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger inFlight = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                int now = inFlight.incrementAndGet();
                peak.accumulateAndGet(now, Math::max);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {
                } finally {
                    inFlight.decrementAndGet();
                    allDone.countDown();
                }
            });
        }
        assertTrue(allDone.await(10, TimeUnit.SECONDS));
        assertTrue(peak.get() <= maxConcurrent, "峰值并发应 ≤ " + maxConcurrent + "，实际 " + peak.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("结构化并发：全部分支成功返回有序结果")
    void testStructuredSuccess() {
        List<String> results = StructuredParallel.invokeAll(Arrays.asList(
                () -> "a",
                () -> {
                    Thread.sleep(20);
                    return "b";
                },
                () -> "c"));
        assertEquals(List.of("a", "b", "c"), results, "结果顺序与分支顺序一致");
    }

    @Test
    @DisplayName("结构化并发：分支失败传播原始异常")
    void testStructuredFailure() {
        IllegalStateException boom = new IllegalStateException("branch-2 boom");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StructuredParallel.invokeAll(Arrays.<Callable<String>>asList(
                        () -> {
                            Thread.sleep(100); // 慢分支应被取消
                            return "slow";
                        },
                        () -> {
                            throw boom;
                        })));
        assertSame(boom, ex.getCause(), "根因为原始异常");
    }

    @Test
    @DisplayName("双分支便捷重载")
    void testPairOverload() {
        Map.Entry<Integer, String> pair = StructuredParallel.invokeAll(() -> 42, () -> "answer");
        assertEquals(42, pair.getKey());
        assertEquals("answer", pair.getValue());
    }
}
