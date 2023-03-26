package com.jvfault.bootstrap;

import com.jvfault.scheduling.annotation.Scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调度测试任务集合（public，供跨包引用）。
 */
public final class Jobs {

    public static volatile CountDownLatch latch;
    public static volatile AtomicInteger ticker;

    private Jobs() {
    }

    @Scheduled(fixedRateMillis = 50, name = "countJob")
    public void count() {
        if (ticker != null) {
            ticker.incrementAndGet();
        }
        if (latch != null) {
            latch.countDown();
        }
    }
}
