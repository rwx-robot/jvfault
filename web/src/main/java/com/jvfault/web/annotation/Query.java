package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * 查询参数绑定（?key=value）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /** 查询键；默认按参数名 */
    String value() default "";
}
