package com.jvfault.cache.cache;

import java.util.Collection;

/**
 * 多级缓存：L1 命中直接返回，L2 命中回填 L1，未命中穿透。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class MultiLevelCache implements Cache {

    private final Cache l1;
    private final Cache l2;

    public MultiLevelCache(Cache l1, Cache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return l1.getName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        T value = l1.get(key, type);
        if (value != null) {
            return value;
        }
        value = l2.get(key, type);
        if (value != null) {
            l1.put(key, value); // 回填 L1
        }
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }

    @Override
    public long size() {
        return l1.size();
    }

    public Cache getL1() {
        return l1;
    }

    public Cache getL2() {
        return l2;
    }

    /** 供测试与监控使用 */
    public Collection<String> underlyingNames() {
        return java.util.Arrays.asList(l1.getName(), l2.getName());
    }
}
