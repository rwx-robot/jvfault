package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 切面优先级，数值越小优先级越高（外层先执行）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {

    int value() default Integer.MAX_VALUE;
}
