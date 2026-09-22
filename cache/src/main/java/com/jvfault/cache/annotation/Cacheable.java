package com.jvfault.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存读取：命中直接返回缓存值，未命中执行方法并写入缓存。
 * 方法所在 Bean 必须实现接口（JDK 代理拦截）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

    /** 缓存名 */
    String[] cacheNames();

    /** 缓存键模板，如 "#p0" / "#p0.id"；默认按 类名#方法名(参数) 生成 */
    String key() default "";
}
