package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * 请求头绑定。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Header {

    /** 请求头名称；默认按参数名 */
    String value() default "";
}
