package com.jvfault.config;

import com.jvfault.config.annotation.ConfigurationProperties;
import com.jvfault.config.annotation.Profile;
import com.jvfault.config.annotation.Value;
import com.jvfault.core.annotation.Component;
import com.jvfault.core.container.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * 配置 Bean 后处理器 - 把 Environment 接入容器初始化序列。
 *
 * <p>beforeInitialization 阶段：
 * <ol>
 *   <li>@Profile 校验（fail-fast）</li>
 *   <li>@Value 字段注入</li>
 *   <li>@ConfigurationProperties 前缀绑定</li>
 * </ol>
 *
 * <p>装配方式（BPP 必须在 Bean 创建前就绪，故由 ConfigModule 以
 * @Bean 工厂创建并容器注入 Environment）：
 * <pre>{@code
 * @Module(imports = ConfigModule.class, providers = {AppService.class})
 * }</pre>
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Component
public class ConfigBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConfigBeanPostProcessor.class);

    private final ConfigurableEnvironment environment;

    public ConfigBeanPostProcessor(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();

        // 1. @Profile fail-fast 校验
        Profile profile = clazz.getAnnotation(Profile.class);
        if (profile != null && !environment.acceptsProfiles(profile.value())) {
            throw new ConfigException("Bean '" + beanName + "' 要求 profile "
                    + java.util.Arrays.toString(profile.value())
                    + "，当前激活: " + java.util.Arrays.toString(environment.getActiveProfiles()));
        }

        // 2. @Value 注入 + 3. @ConfigurationProperties 绑定（本类与父类）
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Value valueAnn = field.getAnnotation(Value.class);
                if (valueAnn != null) {
                    injectValue(bean, field, valueAnn.value());
                }
            }
            c = c.getSuperclass();
        }

        ConfigurationProperties props = clazz.getAnnotation(ConfigurationProperties.class);
        if (props != null) {
            bindProperties(bean, props.value());
        }
        return bean;
    }

    // ============ @Value ============

    private void injectValue(Object bean, Field field, String expression) {
        String resolved = resolveExpression(expression, field.getName());
        Object converted = environment.convert(resolved, field.getType(), field.getName());
        try {
            field.setAccessible(true);
            field.set(bean, converted);
            log.debug("Injected @Value into {}.{} = {}", bean.getClass().getSimpleName(), field.getName(), converted);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("@Value 注入失败: " + field, e.getMessage(), e);
        }
    }

    private String resolveExpression(String expression, String fieldName) {
        String text = expression.trim();
        if (text.startsWith("${") && text.endsWith("}")) {
            String resolved = environment.resolvePlaceholders(text);
            if (resolved.startsWith("${")) {
                throw new ConfigException("@Value 无法解析: " + expression, fieldName);
            }
            return resolved;
        }
        String value = environment.getProperty(text);
        if (value == null) {
            throw new ConfigException("@Value 配置项缺失: " + text, fieldName);
        }
        return value;
    }

    // ============ @ConfigurationProperties ============

    private void bindProperties(Object bean, String prefix) {
        Map<String, Object> flat = snapshotFlat();
        Map<String, Object> sub = PropertyFlattener.subTree(flat, prefix);
        if (sub.isEmpty()) {
            log.debug("No configuration under prefix '{}'", prefix);
            return;
        }
        bindInto(bean, sub, prefix);
    }

    /**
     * 把 sub（前缀已剥离的扁平键值）绑定到 bean 字段。
     * 直接值 -> 类型转换；有子键且字段为 POJO -> 递归绑定。
     */
    private void bindInto(Object bean, Map<String, Object> sub, String prefix) {
        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String key = field.getName();
                Object raw = sub.get(key);
                try {
                    // 集合绑定（key.0 / key.1 ...）
                    if (field.getType() == java.util.List.class && hasSubKeys(sub, key)) {
                        field.setAccessible(true);
                        field.set(bean, new java.util.ArrayList<>(PropertyFlattener.listValues(sub, key)));
                        continue;
                    }

                    // 嵌套 POJO：无直接值但有子键
                    if (raw == null && hasSubKeys(sub, key) && isBindablePojo(field.getType())) {
                        Object nested = field.getType().getDeclaredConstructor().newInstance();
                        bindInto(nested, PropertyFlattener.subTree(sub, key), prefix + "." + key);
                        field.setAccessible(true);
                        field.set(bean, nested);
                        continue;
                    }

                    // 直接值绑定
                    if (raw != null) {
                        Object converted = environment.convert(String.valueOf(raw),
                                field.getType(), prefix + "." + key);
                        field.setAccessible(true);
                        field.set(bean, converted);
                    }
                } catch (ConfigException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ConfigException("配置绑定失败: " + prefix + "." + key,
                            prefix + "." + key, e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean hasSubKeys(Map<String, Object> sub, String key) {
        String dotPrefix = key + ".";
        for (String k : sub.keySet()) {
            if (k.startsWith(dotPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBindablePojo(Class<?> type) {
        if (type.isPrimitive() || type.getName().startsWith("java.")) {
            return false;
        }
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** 汇总所有属性源的扁平视图（按优先级） */
    private Map<String, Object> snapshotFlat() {
        Map<String, Object> flat = new java.util.LinkedHashMap<>();
        List<PropertySource> sources = environment.getPropertySources();
        for (int i = sources.size() - 1; i >= 0; i--) { // 低优先级先放，高优先级覆盖
            PropertySource source = sources.get(i);
            if (source instanceof YamlPropertySource) {
                flat.putAll(((YamlPropertySource) source).getFlat());
            } else if (source instanceof JsonPropertySource) {
                flat.putAll(((JsonPropertySource) source).getFlat());
            }
        }
        return flat;
    }
}
