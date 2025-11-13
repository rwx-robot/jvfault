package com.jvfault.cache;

import com.jvfault.cache.cache.CacheManager;
import com.jvfault.cache.cache.DefaultCacheManager;
import com.jvfault.core.container.FactoryBean;

/**
 * CacheManager 工厂 Bean - 供 CacheModule 装配。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class DefaultCacheManagerProvider implements FactoryBean<CacheManager> {

    @Override
    public CacheManager getObject() {
        return new DefaultCacheManager();
    }
}
