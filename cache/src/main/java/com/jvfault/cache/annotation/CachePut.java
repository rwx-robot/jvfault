package com.jvfault.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存写入：总是执行方法并用返回值更新缓存。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {

    String[] cacheNames();

    String key() default "";
}
