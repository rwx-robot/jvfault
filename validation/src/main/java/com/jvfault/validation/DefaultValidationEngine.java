package com.jvfault.validation;

import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 默认校验引擎 - 委托给 Jakarta Validation（默认 Provider 为 Hibernate Validator）。
 *
 * <p>本框架不再自研约束扫描与反射求值，而是直接复用主流框架（Spring Boot 3 / Quarkus /
 * Jakarta EE）同一套校验 SDK：{@code jakarta.validation} API + Hibernate Validator 引擎。
 * 约束注解改用标准 {@code jakarta.validation.constraints.*}（NotNull / Size / Email / ...）。
 *
 * @since v1.1.0
 * @author jvfault team
 */
public class DefaultValidationEngine implements ValidationEngine {

    private final ValidatorFactory factory;
    private final jakarta.validation.Validator validator;

    public DefaultValidationEngine() {
        this.factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object) {
        if (object == null) {
            throw new IllegalArgumentException("待校验对象不能为 null");
        }
        Set<jakarta.validation.ConstraintViolation<T>> raw = validator.validate(object);
        Set<ConstraintViolation<T>> result = new LinkedHashSet<>();
        for (jakarta.validation.ConstraintViolation<T> v : raw) {
            result.add(toJvfault(v));
        }
        return result;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName) {
        if (object == null) {
            throw new IllegalArgumentException("待校验对象不能为 null");
        }
        Set<jakarta.validation.ConstraintViolation<T>> raw =
                validator.validateProperty(object, propertyName);
        Set<ConstraintViolation<T>> result = new LinkedHashSet<>();
        for (jakarta.validation.ConstraintViolation<T> v : raw) {
            result.add(toJvfault(v));
        }
        return result;
    }

    /** 释放底层 ValidatorFactory（框架关闭时调用，可选）。 */
    public void close() {
        factory.close();
    }

    private <T> ConstraintViolation<T> toJvfault(jakarta.validation.ConstraintViolation<T> v) {
        return new ConstraintViolation<>(
                pathToString(v.getPropertyPath()),
                v.getMessage(),
                v.getInvalidValue(),
                v.getConstraintDescriptor().getAnnotation().annotationType());
    }

    /** 把 jakarta.validation 的 Path 节点序列化为 "address.city" / "items[1].sku" 形式。 */
    private String pathToString(Path path) {
        StringBuilder sb = new StringBuilder();
        for (Path.Node node : path) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(node.getName());
            if (node.getIndex() != null) {
                sb.append('[').append(node.getIndex()).append(']');
            } else if (node.getKey() != null) {
                sb.append('[').append(node.getKey()).append(']');
            }
        }
        return sb.toString();
    }
}
