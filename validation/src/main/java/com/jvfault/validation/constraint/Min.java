package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 数值最小值约束。对应 JSR-380: @Min
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Min {

    long value();

    /** 违反时的消息模板 */
    String message() default "{field} must be at least {value}";
}
