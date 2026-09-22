package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 正则匹配约束。对应 JSR-380: @Pattern
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Pattern {

    String regexp();

    /** 违反时的消息模板 */
    String message() default "{field} must match \"{regexp}\"";
}
