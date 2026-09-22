package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 必须为正数
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Positive {

    /** 违反时的消息模板 */
    String message() default "{field} must be positive";
}
