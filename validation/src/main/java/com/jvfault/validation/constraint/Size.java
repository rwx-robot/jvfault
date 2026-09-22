package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * String/Collection/Map/数组长度范围约束。
 * 对应 JSR-380: @Size
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Size {

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    /** 违反时的消息模板 */
    String message() default "{field} size must be between {min} and {max}";
}
