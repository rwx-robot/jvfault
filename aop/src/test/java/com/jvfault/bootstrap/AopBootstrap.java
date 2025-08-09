package com.jvfault.bootstrap;

import com.jvfault.aop.AopModule;
import com.jvfault.core.annotation.Module;

/**
 * AOP 容器集成测试引导模块。
 * providers 顺序即创建顺序：基础设施 -> 切面 -> 目标。
 */
@Module(imports = AopModule.class, providers = {
        TestAspects.LoggingAspect.class,
        TestAspects.CountingAspect.class,
        Calculator.class
})
public class AopBootstrap {
}
