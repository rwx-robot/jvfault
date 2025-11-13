package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 字符串/集合/Map/数组不得为 null 且 size > 0
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotEmpty {

    /** 违反时的消息模板 */
    String message() default "{field} must not be empty";
}
