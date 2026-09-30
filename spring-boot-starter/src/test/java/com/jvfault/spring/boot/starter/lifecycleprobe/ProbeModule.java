package com.jvfault.spring.boot.starter.lifecycleprobe;

import com.jvfault.core.annotation.Module;

/** 临时探针：生命周期验证专用的根模块。 */
@Module(providers = {SingletonProbe.class, PrototypeProbe.class})
public class ProbeModule {
}
