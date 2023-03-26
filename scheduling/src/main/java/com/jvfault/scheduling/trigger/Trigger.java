package com.jvfault.scheduling.trigger;

import java.time.ZonedDateTime;

/**
 * 触发器 - 计算任务的下一次执行时间。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface Trigger {

    /**
     * @param prev 上次计划执行时间（首次为 null）
     * @param now  当前时间
     * @return 下一次执行时间；null 表示不再执行
     */
    ZonedDateTime nextExecution(ZonedDateTime prev, ZonedDateTime now);
}
