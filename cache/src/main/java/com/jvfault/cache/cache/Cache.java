package com.jvfault.cache.cache;

/**
 * 缓存实例接口。
 * 对应 Spring: Cache；
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface Cache {

    String getName();

    /**
     * 取缓存值；命中返回缓存对象，未命中返回 null。
     */
    <T> T get(Object key, Class<T> type);

    void put(Object key, Object value);

    void evict(Object key);

    void clear();

    long size();
}
