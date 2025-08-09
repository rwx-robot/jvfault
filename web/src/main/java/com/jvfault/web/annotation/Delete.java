package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * HTTP DELETE 路由。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpMethodMapping(value = "DELETE")
public @interface Delete {

    /** 路由路径（相对控制器前缀），支持 :param 与 * 段 */
    String value() default "";
}
