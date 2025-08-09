package com.jvfault.scheduling;

import com.jvfault.core.annotation.Module;

/**
 * 调度模块 - 一站式装配定时任务基础设施。
 *
 * <pre>{@code
 * @Module(imports = SchedulingModule.class, providers = {MyJobs.class})
 * }</pre>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Module(providers = {ScheduledAnnotationBeanPostProcessor.class})
public class SchedulingModule {
}
