package com.jvfault.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * 计时器（环形 reservoir 近似百分位）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class Timer extends Meter {
    private final LongAdder count = new LongAdder();
    private final DoubleAdder totalNanos = new DoubleAdder();
    private volatile long maxNanos;
    private final long[] reservoir = new long[1024];
    private final AtomicInteger reservoirIndex = new AtomicInteger();

    public Timer(Id id) {
        super(id);
    }

    public void record(long nanos) {
        if (nanos < 0 || isNoop()) {
            return;
        }
        count.increment();
        totalNanos.add(nanos);
        synchronized (this) {
            if (nanos > maxNanos) {
                maxNanos = nanos;
            }
        }
        int idx = Math.abs(reservoirIndex.getAndIncrement()) % reservoir.length;
        reservoir[idx] = nanos;
    }

    public void record(java.time.Duration duration) {
        record(duration.toNanos());
    }

    public long count() {
        return count.sum();
    }

    public double totalTimeMillis() {
        return totalNanos.sum() / 1_000_000.0;
    }

    public double meanMillis() {
        long c = count.sum();
        return c == 0 ? 0 : totalTimeMillis() / c;
    }

    public synchronized long maxMillis() {
        return maxNanos / 1_000_000;
    }

    public double percentileMillis(double percentile) {
        int n = reservoirIndex.get();
        if (n == 0) {
            return 0;
        }
        int size = Math.min(n, reservoir.length);
        long[] copy = new long[size];
        System.arraycopy(reservoir, 0, copy, 0, size);
        java.util.Arrays.sort(copy);
        int idx = (int) Math.ceil(percentile * size) - 1;
        idx = Math.min(Math.max(idx, 0), size - 1);
        return copy[idx] / 1_000_000.0;
    }
}
