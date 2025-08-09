package com.jvfault.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 属性扁平化工具 - 把嵌套 Map 展开为 a.b.c 形式的扁平键。
 * 列表使用 key.0 / key.1 下标。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public final class PropertyFlattener {

    private PropertyFlattener() {
    }

    /**
     * 扁平化嵌套 Map。
     */
    public static Map<String, Object> flatten(Map<String, Object> nested) {
        Map<String, Object> result = new LinkedHashMap<>();
        doFlatten("", nested, result);
        return result;
    }

    private static void doFlatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            flattenValue(key, e.getValue(), target);
        }
    }

    @SuppressWarnings("unchecked")
    private static void flattenValue(String key, Object value, Map<String, Object> target) {
        if (value instanceof Map) {
            doFlatten(key, (Map<String, Object>) value, target);
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                flattenValue(key + "." + i, list.get(i), target);
            }
            if (list.isEmpty()) {
                target.put(key, list);
            }
        } else {
            target.put(key, value);
        }
    }

    /**
     * 取扁平 Map 的某前缀子树（用于嵌套绑定）。
     */
    public static Map<String, Object> subTree(Map<String, Object> flat, String prefix) {
        String dotPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
        return flat.entrySet().stream()
                .filter(e -> e.getKey().startsWith(dotPrefix) || e.getKey().equals(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey().equals(prefix) ? e.getKey()
                                : e.getKey().substring(dotPrefix.length()),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * 取扁平 Map 的列表值（key.0, key.1 ...）。
     */
    public static List<Object> listValues(Map<String, Object> flat, String key) {
        Map<String, Object> sub = subTree(flat, key);
        return sub.entrySet().stream()
                .filter(e -> e.getKey().matches("\\d+.*"))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
