package com.jvfault.bootstrap;

import com.jvfault.core.annotation.Module;
import com.jvfault.scheduling.SchedulingModule;

/**
 * 调度测试引导模块。
 */
@Module(imports = SchedulingModule.class, providers = {Jobs.class})
public class SchedulingTestModule {
}
