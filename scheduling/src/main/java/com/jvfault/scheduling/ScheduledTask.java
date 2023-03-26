package com.jvfault.scheduling;

import com.jvfault.scheduling.trigger.Trigger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 已注册的调度任务句柄。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class ScheduledTask {

    public enum State { WAITING, CANCELLED }

    private final String name;
    private final Trigger trigger;
    private final Runnable runnable;
    private volatile ScheduledFuture<?> future;
    private final AtomicInteger executionCount = new AtomicInteger();
    private final AtomicLong lastDurationNanos = new AtomicLong();
    private volatile long lastExecutionMillis;

    ScheduledTask(String name, Trigger trigger, Runnable runnable) {
        this.name = name;
        this.trigger = trigger;
        this.runnable = runnable;
    }

    void bind(ScheduledFuture<?> future) {
        this.future = future;
    }

    Runnable wrappedRunnable() {
        return () -> {
            long start = System.nanoTime();
            try {
                runnable.run();
            } finally {
                lastDurationNanos.set(System.nanoTime() - start);
                lastExecutionMillis = System.currentTimeMillis();
                executionCount.incrementAndGet();
            }
        };
    }

    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
        // 标记状态由 cancel 时 future 完成
    }

    public String getName() { return name; }
    public Trigger getTrigger() { return trigger; }
    public boolean isCancelled() { return future != null && future.isCancelled(); }
    public int getExecutionCount() { return executionCount.get(); }
    public long getLastDurationNanos() { return lastDurationNanos.get(); }
    public long getLastExecutionMillis() { return lastExecutionMillis; }
}
