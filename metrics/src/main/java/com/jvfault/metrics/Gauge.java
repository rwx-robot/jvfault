package com.jvfault.metrics;

import java.util.function.DoubleSupplier;

/**
 * 瞬时值仪表。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class Gauge extends Meter {

    private final DoubleSupplier supplier;

    Gauge(Id id, DoubleSupplier supplier) {
        super(id);
        this.supplier = supplier;
    }

    public double value() {
        return supplier.getAsDouble();
    }
}
