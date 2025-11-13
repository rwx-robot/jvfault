package com.jvfault.scheduling;

import com.jvfault.scheduling.trigger.CronTrigger;
import com.jvfault.scheduling.trigger.PeriodicTrigger;
import com.jvfault.scheduling.trigger.Trigger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于线程池的任务调度器。
 * 对应 Spring: ThreadPoolTaskScheduler
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class ThreadPoolTaskScheduler implements TaskScheduler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ThreadPoolTaskScheduler.class);

    private final ScheduledThreadPoolExecutor executor;

    public ThreadPoolTaskScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ThreadPoolTaskScheduler(int poolSize) {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "jvfault-scheduler-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        this.executor = new ScheduledThreadPoolExecutor(poolSize, factory);
        this.executor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedDelay(Runnable task, long initialDelayMillis, long delayMillis) {
        return executor.scheduleWithFixedDelay(wrap(task), initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
        return executor.scheduleAtFixedRate(wrap(task), initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        if (trigger instanceof PeriodicTrigger) {
            PeriodicTrigger pt = (PeriodicTrigger) trigger;
            return pt.isFixedDelay()
                    ? executor.scheduleWithFixedDelay(wrap(task), pt.getInitialDelayMillis(),
                            pt.getPeriodMillis(), TimeUnit.MILLISECONDS)
                    : executor.scheduleAtFixedRate(wrap(task), pt.getInitialDelayMillis(),
                            pt.getPeriodMillis(), TimeUnit.MILLISECONDS);
        }
        if (trigger instanceof CronTrigger) {
            return scheduleCron(task, (CronTrigger) trigger);
        }
        throw new SchedulingException("不支持的 Trigger 类型: " + trigger.getClass().getName());
    }

    /**
     * Cron 重臂链：每次执行完成后按 Cron 计算下一次并重新入队。
     */
    private ScheduledFuture<?> scheduleCron(Runnable task, CronTrigger cron) {
        RearmingTask rearming = new RearmingTask(wrap(task), cron, executor);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime first = cron.next(now);
        long initialDelay = Math.max(0, Duration.between(now, first).toMillis());
        rearming.arm(executor.schedule(rearming, initialDelay, TimeUnit.MILLISECONDS));
        return rearming;
    }

    private Runnable wrap(final Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                // 调度任务异常必须被捕获，否则周期任务会静默终止
                log.error("Scheduled task failed", t);
            }
        };
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /**
     * 自我重新调度的 Cron 任务 + ScheduledFuture 门面。
     */
    private static final class RearmingTask implements Runnable, ScheduledFuture<Object> {

        private final Runnable delegate;
        private final CronTrigger cron;
        private final ScheduledThreadPoolExecutor executor;
        private final AtomicReference<ScheduledFuture<?>> current = new AtomicReference<>();
        private volatile boolean cancelled;

        RearmingTask(Runnable delegate, CronTrigger cron, ScheduledThreadPoolExecutor executor) {
            this.delegate = delegate;
            this.cron = cron;
            this.executor = executor;
        }

        void arm(ScheduledFuture<?> future) {
            current.set(future);
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }
            try {
                delegate.run();
            } finally {
                if (!cancelled) {
                    ZonedDateTime next = cron.next(ZonedDateTime.now());
                    long delay = Math.max(0, Duration.between(ZonedDateTime.now(), next).toMillis());
                    arm(executor.schedule(this, delay, TimeUnit.MILLISECONDS));
                }
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            ScheduledFuture<?> f = current.get();
            return f != null && f.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override public Object get() { throw new UnsupportedOperationException(); }
        @Override public Object get(long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public long getDelay(TimeUnit unit) {
            ScheduledFuture<?> f = current.get();
            return f != null ? f.getDelay(unit) : 0;
        }
        @Override public int compareTo(java.util.concurrent.Delayed o) { return 0; }
    }
}
