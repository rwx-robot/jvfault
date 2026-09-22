package com.jvfault.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML 属性源（嵌套结构自动扁平化）。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class YamlPropertySource extends PropertySource {

    private final Map<String, Object> flat;

    public YamlPropertySource(String name, InputStream in) {
        super(name);
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(in);
        Map<String, Object> root = loaded instanceof Map ? asMap(loaded) : new LinkedHashMap<String, Object>();
        this.flat = PropertyFlattener.flatten(root);
    }

    public YamlPropertySource(String name, Map<String, Object> flat) {
        super(name);
        this.flat = flat;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    public Map<String, Object> getFlat() {
        return flat;
    }

    @Override
    public String getProperty(String key) {
        Object value = flat.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
