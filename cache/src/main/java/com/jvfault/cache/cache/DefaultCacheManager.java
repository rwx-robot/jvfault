package com.jvfault.cache.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认缓存管理器 - 惰性创建 ConcurrentMapCache。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class DefaultCacheManager implements CacheManager {

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public DefaultCacheManager() {
        this(0);
    }

    public DefaultCacheManager(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> new ConcurrentMapCache(n, ttlMillis));
    }

    /** 注册自定义 Cache 实现 */
    public void registerCache(Cache cache) {
        caches.put(cache.getName(), cache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
}
