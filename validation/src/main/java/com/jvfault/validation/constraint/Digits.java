package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 数字位数约束（整数位/小数位）。对应 JSR-380: @Digits
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Digits {

    int integer();

    int fraction();

    /** 违反时的消息模板 */
    String message() default "{field} numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)";
}
