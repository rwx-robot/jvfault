package com.jvfault.config;

import java.util.Properties;

/**
 * Properties 属性源。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class PropertiesPropertySource extends PropertySource {

    private final Properties properties;

    public PropertiesPropertySource(String name, Properties properties) {
        super(name);
        this.properties = properties;
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
