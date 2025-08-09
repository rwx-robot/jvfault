package com.jvfault.aop;

import com.jvfault.aop.aspect.AspectRegistry;
import com.jvfault.aop.processor.AspectBeanPostProcessor;
import com.jvfault.core.annotation.Module;

/**
 * AOP 模块 - 一站式装配切面基础设施。
 * 将本模块加入应用模块的 imports 即启用 AOP：
 * <pre>{@code
 * @Module(imports = AopModule.class, providers = {MyAspect.class, OrderService.class})
 * }</pre>
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Module(providers = {AspectRegistry.class, AspectBeanPostProcessor.class})
public class AopModule {
}
