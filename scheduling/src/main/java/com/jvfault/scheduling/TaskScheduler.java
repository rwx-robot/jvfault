package com.jvfault.scheduling;

import com.jvfault.scheduling.trigger.Trigger;

import java.util.concurrent.ScheduledFuture;

/**
 * 任务调度器接口。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface TaskScheduler {

    ScheduledFuture<?> scheduleAtFixedDelay(Runnable task, long initialDelayMillis, long delayMillis);

    ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis);

    ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

    void shutdown();
}
