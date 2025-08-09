package com.jvfault.aot;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GraalVM reflect-config.json 生成器 —— 为容器 Bean 生成
 * 反射注册条目（native-image 需要）。
 * 对应 roadmap v0.9.0 AOT 反射注册
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class ReflectConfigGenerator {

    /**
     * 为指定 Bean 类生成 reflect-config 条目（含全方法）。
     */
    public Map<String, Object> entryFor(Class<?> beanClass) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", beanClass.getName());
        entry.put("allDeclaredConstructors", true);
        entry.put("allPublicConstructors", true);
        entry.put("allDeclaredMethods", true);
        entry.put("allPublicMethods", true);
        return entry;
    }

    /**
     * 生成多个 Bean 的 JSON。
     */
    public String toJson(List<Class<?>> beanClasses) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < beanClasses.size(); i++) {
            sb.append("  ").append(compactJson(entryFor(beanClasses.get(i))));
            if (i < beanClasses.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(']');
        return sb.toString();
    }

    private String compactJson(Map<String, Object> entry) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : entry.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append('"').append(e.getKey()).append("\":");
            if (e.getValue() instanceof Boolean) {
                sb.append(e.getValue());
            } else {
                sb.append('"').append(e.getValue()).append('"');
            }
        }
        return sb.append('}').toString();
    }
}
