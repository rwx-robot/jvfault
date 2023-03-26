package com.jvfault.scheduling;

import com.jvfault.scheduling.trigger.CronTrigger;
import com.jvfault.bootstrap.AmbiguousModule;
import com.jvfault.bootstrap.Jobs;
import com.jvfault.bootstrap.SchedulingTestModule;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-scheduling 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("Scheduling 模块测试")
class SchedulingModuleTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    // ============ CronParser ============

    @Test
    @DisplayName("cron: 每日整点匹配")
    void testCronDailyAtMidnight() {
        CronTrigger cron = new CronTrigger("0 0 0 * * *");
        ZonedDateTime now = at(2021, 5, 10, 15, 30, 45);
        ZonedDateTime next = cron.next(now);
        assertEquals(at(2021, 5, 11, 0, 0, 0), next);
    }

    @Test
    @DisplayName("cron: 列表与范围")
    void testCronListAndRange() {
        CronTrigger cron = new CronTrigger("0 0,30 9-17 * * MON-FRI");
        ZonedDateTime fridayEvening = at(2021, 5, 14, 18, 0, 0); // 周五 18:00
        ZonedDateTime next = cron.next(fridayEvening);
        // 18 点超出 9-17 -> 下一个工作日周一 9:00
        assertEquals(at(2021, 5, 17, 9, 0, 0), next);
        assertTrue(cron.matches(at(2021, 5, 17, 9, 30, 0)));
        assertFalse(cron.matches(at(2021, 5, 17, 9, 31, 0)));
    }

    @Test
    @DisplayName("cron: 步进")
    void testCronStep() {
        CronTrigger cron = new CronTrigger("*/15 * * * * *"); // 每 15 秒
        ZonedDateTime now = at(2021, 5, 10, 12, 0, 3);
        assertEquals(at(2021, 5, 10, 12, 0, 15), cron.next(now));
    }

    @Test
    @DisplayName("cron: 英文缩写月份与星期")
    void testCronNames() {
        CronTrigger cron = new CronTrigger("0 0 12 1 JAN,JUL SUN");
        assertTrue(cron.matches(at(2021, 1, 3, 12, 0, 0)));  // 1月3日周日
        assertFalse(cron.matches(at(2021, 2, 1, 12, 0, 0))); // 2月1日周一
    }

    @Test
    @DisplayName("cron: 5 字段自动补秒")
    void testCronFiveFields() {
        CronTrigger cron = new CronTrigger("30 3 * * *"); // 每天 03:30
        ZonedDateTime now = at(2021, 5, 10, 12, 0, 0);
        assertEquals(at(2021, 5, 11, 3, 30, 0), cron.next(now));
    }

    @Test
    @DisplayName("cron: 非法表达式抛 SchedulingException")
    void testCronIllegal() {
        assertThrows(SchedulingException.class, () -> new CronTrigger("* * * *"));
        assertThrows(SchedulingException.class, () -> new CronTrigger("61 0 0 * * *"));
        assertThrows(SchedulingException.class, () -> new CronTrigger("0 0 0 * * 0/0"));
    }

    private ZonedDateTime at(int y, int m, int d, int h, int min, int s) {
        return ZonedDateTime.of(y, m, d, h, min, s, 0, ZONE);
    }

    // ============ 周期执行 ============

    @Test
    @DisplayName("fixedRate 按频率执行")
    void testFixedRate() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler(2);
        try {
            AtomicInteger count = new AtomicInteger();
            scheduler.scheduleAtFixedRate(count::incrementAndGet, 0, 50);
            Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> count.get() >= 3);
            assertTrue(count.get() >= 3);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    @DisplayName("fixedDelay 语义执行")
    void testFixedDelay() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler(2);
        try {
            AtomicInteger count = new AtomicInteger();
            scheduler.scheduleAtFixedDelay(count::incrementAndGet, 20, 40);
            Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> count.get() >= 2);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    @DisplayName("任务异常不中断周期调度")
    void testExceptionDoesNotStopSchedule() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler(2);
        try {
            AtomicInteger count = new AtomicInteger();
            scheduler.scheduleAtFixedRate(() -> {
                count.incrementAndGet();
                throw new RuntimeException("boom");
            }, 0, 50);
            Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> count.get() >= 3);
            assertTrue(count.get() >= 3, "异常后应继续执行");
        } finally {
            scheduler.shutdown();
        }
    }

    // ============ @Scheduled 与容器集成 ============

    @Test
    @DisplayName("@Scheduled 扫描注册与执行")
    void testScheduledAnnotation() throws Exception {
        Jobs.latch = new CountDownLatch(3);
        Jobs.ticker = new AtomicInteger();

        com.jvfault.core.module.ModuleContainer container =
                com.jvfault.core.bootstrap.JvfaultApplication.createContainer(SchedulingTestModule.class);
        try {
            assertTrue(Jobs.latch.await(5, TimeUnit.SECONDS), "任务应在 5 秒内执行 3 次");

            ScheduledAnnotationBeanPostProcessor bpp =
                    container.getBeanRegistry().getBean(ScheduledAnnotationBeanPostProcessor.class);
            assertEquals(1, bpp.getScheduledTasks().size());
            assertEquals("countJob", bpp.getScheduledTasks().get(0).getName());
        } finally {
            container.destroy();
        }
    }

    @Test
    @DisplayName("@Scheduled 多注解互斥校验")
    void testScheduledAmbiguous() {
        // 容器 refresh 会包装异常，但根因是 SchedulingException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            com.jvfault.core.module.ModuleContainer container =
                    com.jvfault.core.bootstrap.JvfaultApplication.createContainer(AmbiguousModule.class);
            container.destroy();
        });
        Throwable cause = ex.getCause();
        while (cause != null && !(cause instanceof SchedulingException)) {
            cause = cause.getCause();
        }
        assertNotNull(cause, "根因应为 SchedulingException");
    }

    @Test
    @DisplayName("shutdown 后调度器停止")
    void testShutdown() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler(1);
        scheduler.shutdown();
        assertTrue(scheduler.isShutdown());
    }
}
