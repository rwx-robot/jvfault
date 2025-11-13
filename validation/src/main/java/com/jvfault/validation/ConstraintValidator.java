package com.jvfault.validation;

import java.lang.annotation.Annotation;

/**
 * 自定义约束校验器 SPI。
 *
 * @param <A> 约束注解类型
 * @param <T> 被校验值类型
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface ConstraintValidator<A extends Annotation, T> {

    /**
     * @return true 表示校验通过
     */
    boolean isValid(T value, ValidationContext context);
}
