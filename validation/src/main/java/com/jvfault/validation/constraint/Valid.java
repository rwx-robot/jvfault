package com.jvfault.validation.constraint;

import java.lang.annotation.*;

/**
 * 级联校验标记：校验器递归校验被标注字段/ getter 的对象。
 * 对应 JSR-380: @Valid
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Valid {
}
