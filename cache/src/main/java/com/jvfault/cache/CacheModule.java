package com.jvfault.cache;

import com.jvfault.core.annotation.Module;

/**
 * 缓存模块 - 装配缓存管理器与注解拦截。
 *
 * <pre>{@code
 * @Module(imports = CacheModule.class, providers = {UserService.class})
 * }</pre>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Module(providers = {DefaultCacheManagerProvider.class, CacheableBeanPostProcessor.class})
public class CacheModule {
}
