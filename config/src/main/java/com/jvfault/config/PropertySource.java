package com.jvfault.config;

/**
 * 属性源抽象 - 有序配置源集合中的一层。
 * 对应 Spring: PropertySource
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public abstract class PropertySource {

    private final String name;

    protected PropertySource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getProperty(String key);

    public boolean containsProperty(String key) {
        return getProperty(key) != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
