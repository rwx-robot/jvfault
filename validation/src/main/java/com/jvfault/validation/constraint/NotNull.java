package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 元素不得为 null
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotNull {

    /** 违反时的消息模板 */
    String message() default "{field} must not be null";
}
