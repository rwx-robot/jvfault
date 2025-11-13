package com.jvfault.validation;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 校验上下文 - 向 ConstraintValidator 提供注解属性与消息插值能力。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ValidationContext {

    private final String propertyPath;
    private final Annotation annotation;
    private final Map<String, Object> annotationAttributes;

    public ValidationContext(String propertyPath, Annotation annotation, Map<String, Object> annotationAttributes) {
        this.propertyPath = propertyPath;
        this.annotation = annotation;
        this.annotationAttributes = annotationAttributes;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    /** 注解属性值（如 min/max/regexp/value） */
    public Object getAttribute(String name) {
        return annotationAttributes.get(name);
    }

    public int intAttribute(String name) {
        return ((Number) annotationAttributes.get(name)).intValue();
    }

    public long longAttribute(String name) {
        return ((Number) annotationAttributes.get(name)).longValue();
    }

    public String stringAttribute(String name) {
        Object v = annotationAttributes.get(name);
        return v != null ? v.toString() : null;
    }

    /**
     * 消息插值：{field} -> 属性路径，{attr} -> 注解属性。
     */
    public String interpolate(String template) {
        String result = template.replace("{field}", propertyPath);
        for (Map.Entry<String, Object> e : new LinkedHashMap<>(annotationAttributes).entrySet()) {
            result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }
}
