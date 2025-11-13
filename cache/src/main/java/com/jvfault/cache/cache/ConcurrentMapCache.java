package com.jvfault.cache.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的缓存（可选 TTL，惰性淘汰）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class ConcurrentMapCache implements Cache {

    private final String name;
    private final long ttlMillis;
    private final ConcurrentHashMap<Object, ValueHolder> store = new ConcurrentHashMap<>();

    public ConcurrentMapCache(String name) {
        this(name, 0);
    }

    public ConcurrentMapCache(String name, long ttlMillis) {
        this.name = name;
        this.ttlMillis = ttlMillis;
    }

    private static final class ValueHolder {
        final Object value;
        final long expireAt;

        ValueHolder(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean expired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueHolder holder = store.get(key);
        if (holder == null) {
            return null;
        }
        if (holder.expired()) {
            store.remove(key);
            return null;
        }
        return (T) holder.value;
    }

    @Override
    public void put(Object key, Object value) {
        long expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;
        store.put(key, new ValueHolder(value, expireAt));
    }

    @Override
    public void evict(Object key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public long size() {
        long count = 0;
        for (ValueHolder holder : store.values()) {
            if (!holder.expired()) {
                count++;
            }
        }
        return count;
    }

    Map<Object, Object> asMap() {
        Map<Object, Object> out = new LinkedHashMap<>();
        store.forEach((k, v) -> {
            if (!v.expired()) {
                out.put(k, v.value);
            }
        });
        return out;
    }
}
