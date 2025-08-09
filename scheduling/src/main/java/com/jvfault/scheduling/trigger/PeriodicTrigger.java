package com.jvfault.scheduling.trigger;

/**
 * 周期触发器。
 *
 * <p>fixedDelay：上次「完成」后延迟；fixedRate：从上次「开始」固定间隔。
 * 简化实现统一按上次计划时间推算。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class PeriodicTrigger implements Trigger {

    private final long periodMillis;
    private final long initialDelayMillis;
    private final boolean fixedDelay;

    public PeriodicTrigger(long periodMillis, long initialDelayMillis, boolean fixedDelay) {
        this.periodMillis = periodMillis;
        this.initialDelayMillis = initialDelayMillis;
        this.fixedDelay = fixedDelay;
    }

    public static PeriodicTrigger fixedDelay(long periodMillis) {
        return new PeriodicTrigger(periodMillis, 0, true);
    }

    public static PeriodicTrigger fixedRate(long periodMillis) {
        return new PeriodicTrigger(periodMillis, 0, false);
    }

    @Override
    public java.time.ZonedDateTime nextExecution(java.time.ZonedDateTime prev, java.time.ZonedDateTime now) {
        if (prev == null) {
            return now.plus(initialDelayMillis, java.time.temporal.ChronoUnit.MILLIS);
        }
        // fixedDelay 语义需要完成时间，线程池层面按计划时间近似
        return prev.plus(periodMillis, java.time.temporal.ChronoUnit.MILLIS);
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    public boolean isFixedDelay() {
        return fixedDelay;
    }
}
