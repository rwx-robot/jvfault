package com.jvfault.spring.boot.starter.lifecycleprobe;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 临时探针：统计 jvfault 自有 @Component 的生命周期回调被调用次数。
 * 验证完毕后连同整个生命周期探针包一起删除。
 */
@com.jvfault.core.annotation.Component
public class SingletonProbe {

    public static int constructCount;
    public static int destroyCount;

    public SingletonProbe() {
    }

    @PostConstruct
    public void init() {
        constructCount++;
    }

    @PreDestroy
    public void shutdown() {
        destroyCount++;
    }

    public static void reset() {
        constructCount = 0;
        destroyCount = 0;
    }
}
