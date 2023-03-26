package com.jvfault.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * 计数器。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class Counter extends Meter {
    private final LongAdder value = new LongAdder();

    public Counter(Id id) {
        super(id);
    }

    public void increment() {
        increment(1);
    }

    public void increment(double amount) {
        if (isNoop()) {
            return;
        }
        value.add((long) amount);
    }

    public double count() {
        return value.sum();
    }
}
