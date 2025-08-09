package com.jvfault.config;

/**
 * JVM 系统属性源 (System.getProperties)。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class SystemPropertiesPropertySource extends PropertySource {

    public SystemPropertiesPropertySource() {
        super("systemProperties");
    }

    @Override
    public String getProperty(String key) {
        return System.getProperty(key);
    }
}
