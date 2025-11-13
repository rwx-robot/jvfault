package com.jvfault.validation;

import com.jvfault.validation.constraint.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认校验引擎 - 反射扫描字段与 getter 上的约束注解。
 *
 * <p>内置约束：NotNull/NotBlank/NotEmpty/Size/Min/Max/Pattern/Email/
 * Digits/AssertTrue/AssertFalse/Positive/PositiveOrZero/Negative/NegativeOrZero/
 * Past/Future，以及 Valid 级联。
 * 自定义约束通过 {@link #registerValidator(Class, ConstraintValidator)} 注册。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class DefaultValidationEngine implements ValidationEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultValidationEngine.class);

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final Map<Class<? extends Annotation>, ConstraintValidator<?, ?>> customValidators =
            new ConcurrentHashMap<>();

    /** 类级约束元数据缓存 */
    private final Map<Class<?>, List<ConstraintElement>> metadataCache = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Set<ConstraintViolation<T>> validate(T object) {
        assertNotNull(object);
        Set violations = new LinkedHashSet();
        collectViolations(object, "", violations, new IdentityHashMap());
        return violations;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName) {
        assertNotNull(object);
        Set violations = new LinkedHashSet();
        List<ConstraintElement> elements = metadataFor(object.getClass());
        for (ConstraintElement element : elements) {
            if (!element.name.equals(propertyName)) {
                continue;
            }
            Object value = element.read(object);
            checkConstraints(object, element, value, propertyName, violations, new IdentityHashMap());
        }
        return violations;
    }

    /**
     * 注册自定义约束校验器。
     */
    public <A extends Annotation, T> void registerValidator(
            Class<A> annotationType, ConstraintValidator<A, T> validator) {
        customValidators.put(annotationType, validator);
        metadataCache.clear();
    }

    // ============ 内部实现 ============

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void collectViolations(Object object, String pathPrefix,
                                   Set violations,
                                   Map visited) {
        if (visited.put(object, Boolean.TRUE) != null) {
            return; // 循环引用防护
        }
        for (ConstraintElement element : (List<ConstraintElement>) metadataFor(object.getClass())) {
            Object value = element.read(object);
            String path = pathPrefix + element.name;
            checkConstraints(object, element, value, path, violations, visited);

            // @Valid 级联
            if (element.cascade && value != null) {
                if (value instanceof Collection) {
                    int i = 0;
                    for (Object item : (Collection<?>) value) {
                        if (item != null) {
                            collectViolations(item, path + "[" + (i++) + "].", violations, visited);
                        } else {
                            i++;
                        }
                    }
                } else if (value.getClass().getName().startsWith("java.")) {
                    log.debug("Skip cascade for JDK type: {}", value.getClass().getName());
                } else {
                    collectViolations(value, path + ".", violations, visited);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void checkConstraints(Object root, ConstraintElement element, Object value, String path,
                                  Set violations,
                                  Map visited) {
        for (Annotation annotation : element.constraints) {
            Class<? extends Annotation> type = annotation.annotationType();
            ValidationContext ctx = new ValidationContext(path, annotation, attributesOf(annotation));
            boolean valid = checkBuiltin(type, annotation, value, ctx)
                    && checkCustom(type, value, ctx);
            if (!valid) {
                violations.add(new ConstraintViolation<>(
                        path, ctx.interpolate(messageOf(annotation)), value, type));
            }
        }
    }

    private boolean checkCustom(Class<? extends Annotation> type, Object value, ValidationContext ctx) {
        ConstraintValidator validator = customValidators.get(type);
        if (validator == null) {
            return true;
        }
        return validator.isValid(value, ctx);
    }

    /** 内置约束校验；非内置注解返回 true（交给自定义校验器） */
    private boolean checkBuiltin(Class<? extends Annotation> type, Annotation ann, Object value,
                                 ValidationContext ctx) {
        if (type == NotNull.class) {
            return value != null;
        }
        if (type == NotBlank.class) {
            return value instanceof String && !((String) value).trim().isEmpty();
        }
        if (type == NotEmpty.class) {
            return sizeOf(value) != null && sizeOf(value) > 0;
        }
        if (type == Size.class) {
            if (value == null) return true;
            Integer size = sizeOf(value);
            return size != null && size >= ctx.intAttribute("min") && size <= ctx.intAttribute("max");
        }
        if (type == Min.class) {
            return value == null || numberOf(value) >= ctx.longAttribute("value");
        }
        if (type == Max.class) {
            return value == null || numberOf(value) <= ctx.longAttribute("value");
        }
        if (type == Pattern.class) {
            return value == null || ((String) value).matches(ctx.stringAttribute("regexp"));
        }
        if (type == Email.class) {
            return value == null || ((String) value).matches(EMAIL_REGEX);
        }
        if (type == Digits.class) {
            if (value == null) return true;
            String text = new java.math.BigDecimal(numberOf(value)).toPlainString();
            String[] parts = text.split("\\.");
            int intDigits = parts[0].replace("-", "").length();
            int fracDigits = parts.length > 1 ? parts[1].length() : 0;
            return intDigits <= ctx.intAttribute("integer") && fracDigits <= ctx.intAttribute("fraction");
        }
        if (type == AssertTrue.class) {
            return Boolean.TRUE.equals(value);
        }
        if (type == AssertFalse.class) {
            return !Boolean.TRUE.equals(value);
        }
        if (type == Positive.class) {
            return value == null || numberOf(value) > 0;
        }
        if (type == PositiveOrZero.class) {
            return value == null || numberOf(value) >= 0;
        }
        if (type == Negative.class) {
            return value == null || numberOf(value) < 0;
        }
        if (type == NegativeOrZero.class) {
            return value == null || numberOf(value) <= 0;
        }
        if (type == Past.class) {
            return value == null || compareToNow(value) < 0;
        }
        if (type == Future.class) {
            return value == null || compareToNow(value) > 0;
        }
        if (type == Valid.class) {
            return true; // 标记注解，级联由 collectViolations 处理
        }
        return true; // 非内置注解交由自定义校验器
    }

    private Integer sizeOf(Object value) {
        if (value instanceof String) return ((String) value).length();
        if (value instanceof Collection) return ((Collection<?>) value).size();
        if (value instanceof Map) return ((Map<?, ?>) value).size();
        if (value != null && value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return null;
    }

    private long numberOf(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private int compareToNow(Object value) {
        if (value instanceof Temporal) {
            Temporal temporal = (Temporal) value;
            if (temporal instanceof Instant) {
                return ((Instant) temporal).compareTo(Instant.now());
            }
            if (temporal instanceof LocalDate) {
                return ((LocalDate) temporal).compareTo(LocalDate.now());
            }
            if (temporal instanceof LocalDateTime) {
                return ((LocalDateTime) temporal).compareTo(LocalDateTime.now());
            }
            if (temporal instanceof ZonedDateTime) {
                return ((ZonedDateTime) temporal).toInstant().compareTo(Instant.now());
            }
        }
        if (value instanceof Date) {
            return ((Date) value).compareTo(new Date());
        }
        throw new IllegalArgumentException("不支持的时间类型: " + value.getClass());
    }

    private String messageOf(Annotation annotation) {
        try {
            Object v = annotation.annotationType().getMethod("message").invoke(annotation);
            return v != null ? v.toString() : "invalid";
        } catch (Exception e) {
            return "invalid";
        }
    }

    private Map<String, Object> attributesOf(Annotation annotation) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        for (Method m : annotation.annotationType().getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && !m.isDefault()) {
                try {
                    attrs.put(m.getName(), m.invoke(annotation));
                } catch (Exception ignored) {
                }
            } else if (m.getParameterCount() == 0 && m.isDefault()) {
                try {
                    attrs.put(m.getName(), m.invoke(annotation));
                } catch (Exception ignored) {
                }
            }
        }
        return attrs;
    }

    private void assertNotNull(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("待校验对象不能为 null");
        }
    }

    // ============ 元数据 ============

    private List<ConstraintElement> metadataFor(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, this::buildMetadata);
    }

    private List<ConstraintElement> buildMetadata(Class<?> clazz) {
        List<ConstraintElement> elements = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                addElement(elements, field.getName(), field.getType(), field.getAnnotations(), field);
            }
            for (Method method : c.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    continue;
                }
                String name = method.getName();
                boolean getter = (name.startsWith("get") && name.length() > 3)
                        || (name.startsWith("is") && name.length() > 2);
                if (!getter) {
                    continue;
                }
                String propName = name.startsWith("is")
                        ? decapitalize(name.substring(2)) : decapitalize(name.substring(3));
                addElement(elements, propName, method.getReturnType(), method.getAnnotations(), method);
            }
            c = c.getSuperclass();
        }
        return elements;
    }

    private void addElement(List<ConstraintElement> elements, String name, Class<?> type,
                            Annotation[] annotations, Object accessor) {
        List<Annotation> constraints = new ArrayList<>();
        boolean cascade = false;
        for (Annotation ann : annotations) {
            if (ann.annotationType() == Valid.class) {
                cascade = true;
            } else if (isConstraint(ann.annotationType())) {
                constraints.add(ann);
            }
        }
        if (!constraints.isEmpty() || cascade) {
            elements.add(new ConstraintElement(name, type, constraints, cascade, accessor));
        }
    }

    private boolean isConstraint(Class<? extends Annotation> type) {
        Package pkg = type.getPackage();
        if (pkg != null && pkg.getName().startsWith("com.jvfault.validation.constraint")) {
            return true;
        }
        // 注册了自定义校验器的注解同样视为约束
        return customValidators.containsKey(type);
    }

    private String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /** 类字段/getter 的约束元数据 */
    private static class ConstraintElement {
        final String name;
        final Class<?> type;
        final List<Annotation> constraints;
        final boolean cascade;
        final Object accessor; // Field 或 Method

        ConstraintElement(String name, Class<?> type, List<Annotation> constraints,
                          boolean cascade, Object accessor) {
            this.name = name;
            this.type = type;
            this.constraints = constraints;
            this.cascade = cascade;
            this.accessor = accessor;
        }

        Object read(Object target) {
            try {
                if (accessor instanceof Field) {
                    ((Field) accessor).setAccessible(true);
                    return ((Field) accessor).get(target);
                }
                ((Method) accessor).setAccessible(true);
                return ((Method) accessor).invoke(target);
            } catch (Exception e) {
                throw new IllegalStateException("无法读取属性: " + name, e);
            }
        }
    }
}
