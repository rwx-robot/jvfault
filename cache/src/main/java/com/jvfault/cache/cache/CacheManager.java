package com.jvfault.cache.cache;

import java.util.Collection;

/**
 * 缓存管理器 - 缓存实例的注册与惰性创建。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface CacheManager {

    Cache getCache(String name);

    Collection<String> getCacheNames();
}
