package com.jvfault.bootstrap;

import com.jvfault.core.annotation.Module;
import com.jvfault.scheduling.SchedulingModule;

/**
 * 多注解互斥的非法模块。
 */
@Module(imports = SchedulingModule.class, providers = {AmbiguousJob.class})
public class AmbiguousModule {
}
