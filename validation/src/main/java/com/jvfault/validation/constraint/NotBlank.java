package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 字符串不得为 null 且必须含非空白字符
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotBlank {

    /** 违反时的消息模板 */
    String message() default "{field} must not be blank";
}
