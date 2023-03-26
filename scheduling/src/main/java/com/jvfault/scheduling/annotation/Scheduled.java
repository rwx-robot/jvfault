package com.jvfault.scheduling.annotation;

import java.lang.annotation.*;

/**
 * 定时任务注解（方法级）。fixedDelay / fixedRate / cron 三选一。
 * 对应 Spring: @Scheduled；
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {

    /** 固定延迟（上次完成后）毫秒数 */
    long fixedDelayMillis() default -1;

    /** 固定频率（上次开始时间起）毫秒数 */
    long fixedRateMillis() default -1;

    /** 首次执行延迟毫秒数 */
    long initialDelayMillis() default 0;

    /** Cron 表达式（5 或 6 字段） */
    String cron() default "";

    /** 任务名，默认 类名.方法名 */
    String name() default "";
}
