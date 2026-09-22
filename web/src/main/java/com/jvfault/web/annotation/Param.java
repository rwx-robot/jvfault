package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * 路径参数绑定（:param）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /** 路径参数名；默认按参数名（需 -parameters 编译） */
    String value() default "";
}
