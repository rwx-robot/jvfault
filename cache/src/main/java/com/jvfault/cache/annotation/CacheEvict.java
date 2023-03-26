package com.jvfault.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存淘汰：方法执行后驱逐缓存。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {

    String[] cacheNames();

    String key() default "";

    /** true 时清空整个缓存 */
    boolean allEntries() default false;
}
