package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * 控制器 - 路由入口类。value() 为该控制器全部路由的前缀。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    /** 路由前缀 */
    String value() default "";
}
