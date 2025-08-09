package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * HTTP 路由方法注解（GET/POST/PUT/DELETE/PATCH 的元注解）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpMethodMapping {

    /** HTTP 方法名（大写） */
    String value();

    /** 路由路径（相对控制器前缀），支持 :param 与 * 段 */
    String path() default "";
}
