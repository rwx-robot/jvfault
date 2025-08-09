package com.jvfault.scheduling;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.container.BeanPostProcessor;
import com.jvfault.scheduling.annotation.Scheduled;
import com.jvfault.scheduling.trigger.CronTrigger;
import com.jvfault.scheduling.trigger.PeriodicTrigger;
import com.jvfault.scheduling.trigger.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Scheduled 注解后处理器 - 扫描 Bean 的定时任务方法并注册到调度器。
 *
 * <p>调度器选择：优先使用容器中已实例化的 {@link TaskScheduler} Bean；
 * 否则使用内置默认线程池（CPU 核数）。
 *
 * <p>注册方式：
 * <pre>{@code
 * @Module(imports = SchedulingModule.class, providers = {Jobs.class})
 * }</pre>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Component
public class ScheduledAnnotationBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAnnotationBeanPostProcessor.class);

    private volatile TaskScheduler scheduler;
    private final CopyOnWriteArrayList<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private volatile boolean ownsScheduler;

    public ScheduledAnnotationBeanPostProcessor() {
        this(null);
    }

    public ScheduledAnnotationBeanPostProcessor(TaskScheduler scheduler) {
        this.scheduler = scheduler;
        this.ownsScheduler = scheduler == null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                Scheduled ann = method.getAnnotation(Scheduled.class);
                if (ann != null) {
                    register(bean, method, ann);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return bean;
    }

    private synchronized void register(Object bean, Method method, Scheduled ann) {
        long fixedDelay = ann.fixedDelayMillis();
        long fixedRate = ann.fixedRateMillis();
        String cron = ann.cron();

        int configured = 0;
        if (fixedDelay >= 0) configured++;
        if (fixedRate >= 0) configured++;
        if (!cron.isEmpty()) configured++;
        if (configured != 1) {
            throw new SchedulingException("@Scheduled 需要且只能配置 fixedDelayMillis/fixedRateMillis/cron 之一: "
                    + method);
        }

        String name = ann.name().isEmpty()
                ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
                : ann.name();

        Trigger trigger;
        long initialDelay = ann.initialDelayMillis();
        if (fixedDelay >= 0) {
            trigger = new PeriodicTrigger(fixedDelay, initialDelay, true);
        } else if (fixedRate >= 0) {
            trigger = new PeriodicTrigger(fixedRate, initialDelay, false);
        } else {
            trigger = new CronTrigger(cron);
        }

        method.setAccessible(true);
        ScheduledTask task = new ScheduledTask(name, trigger, () -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                throw new SchedulingException("任务执行失败: " + name, e);
            }
        });

        doSchedule(task);
        tasks.add(task);
        log.info("Registered scheduled task: {}", name);
    }

    /** 供任务注册时绑定 future（内部协作类） */
    void doSchedule(ScheduledTask task) {
        ensureScheduler();
        java.util.concurrent.ScheduledFuture<?> future = scheduler.schedule(
                task.wrappedRunnable(), task.getTrigger());
        task.bind(future);
    }

    private void ensureScheduler() {
        if (scheduler == null) {
            synchronized (this) {
                if (scheduler == null) {
                    scheduler = new ThreadPoolTaskScheduler();
                    ownsScheduler = true;
                }
            }
        }
    }

    /**
     * 容器关闭时停止调度（可由 ops 模块的优雅关闭调用）。
     */
    public void destroy() {
        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
        if (ownsScheduler && scheduler != null) {
            scheduler.shutdown();
        }
    }

    public List<ScheduledTask> getScheduledTasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }
}

