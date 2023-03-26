package com.jvfault.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map 属性源。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class MapPropertySource extends PropertySource {

    private final Map<String, Object> source;

    public MapPropertySource(String name, Map<String, Object> source) {
        super(name);
        this.source = source;
    }

    public Map<String, Object> getSource() {
        return source;
    }

    @Override
    public String getProperty(String key) {
        Object value = source.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
