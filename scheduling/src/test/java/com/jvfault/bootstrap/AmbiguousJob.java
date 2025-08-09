package com.jvfault.bootstrap;

import com.jvfault.core.annotation.Component;
import com.jvfault.scheduling.annotation.Scheduled;

/**
 * fixedDelay 与 cron 同时配置 —— 应抛 SchedulingException。
 */
@Component
public class AmbiguousJob {

    @Scheduled(fixedDelayMillis = 100, cron = "0 * * * * *")
    public void ambiguous() {
    }
}
