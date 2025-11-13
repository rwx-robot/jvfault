package com.jvfault.validation;

import java.util.Set;

/**
 * 校验引擎接口。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface ValidationEngine {

    /**
     * 校验对象全部字段与 getter。
     *
     * @return 违反集合（空集 = 通过）
     */
    <T> Set<ConstraintViolation<T>> validate(T object);

    /**
     * 校验单个属性。
     */
    <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName);
}
