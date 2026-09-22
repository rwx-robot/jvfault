package com.jvfault.validation;

/**
 * 单个约束校验失败结果。
 * 对应 JSR-380: ConstraintViolation
 *
 * @param <T> 被校验对象的类型
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ConstraintViolation<T> {

    private final String propertyPath;
    private final String message;
    private final Object invalidValue;
    private final Class<?> constraintType;

    public ConstraintViolation(String propertyPath, String message, Object invalidValue, Class<?> constraintType) {
        this.propertyPath = propertyPath;
        this.message = message;
        this.invalidValue = invalidValue;
        this.constraintType = constraintType;
    }

    /** 属性路径，如 "address.city" 或 "items[0].name" */
    public String getPropertyPath() {
        return propertyPath;
    }

    /** 插值后的消息 */
    public String getMessage() {
        return message;
    }

    /** 非法值 */
    public Object getInvalidValue() {
        return invalidValue;
    }

    /** 约束注解类型 */
    public Class<?> getConstraintType() {
        return constraintType;
    }

    @Override
    public String toString() {
        return propertyPath + ": " + message;
    }
}
