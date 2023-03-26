package com.jvfault.cache.cache;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 基于 Caffeine 的缓存（W-TinyLFU 淘汰 + TTL）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class CaffeineCache implements Cache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

    public CaffeineCache(String name, long ttlMillis, long maximumSize) {
        this.name = name;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (ttlMillis > 0) {
            builder.expireAfterWrite(ttlMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        if (maximumSize > 0) {
            builder.maximumSize(maximumSize);
        }
        this.cache = builder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        return (T) cache.getIfPresent(key);
    }

    @Override
    public void put(Object key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }
}
