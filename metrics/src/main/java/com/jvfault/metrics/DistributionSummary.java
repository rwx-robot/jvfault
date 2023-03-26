package com.jvfault.metrics;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * 分布式取样统计（count/total/max）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class DistributionSummary extends Meter {
    private final LongAdder count = new LongAdder();
    private final DoubleAdder total = new DoubleAdder();
    private volatile long max;

    public DistributionSummary(Id id) {
        super(id);
    }

    public void record(long amount) {
        if (amount < 0 || isNoop()) {
            return;
        }
        count.increment();
        total.add(amount);
        synchronized (this) {
            if (amount > max) {
                max = amount;
            }
        }
    }

    public long count() {
        return count.sum();
    }

    public double totalAmount() {
        return total.sum();
    }

    public long max() {
        return max;
    }
}
