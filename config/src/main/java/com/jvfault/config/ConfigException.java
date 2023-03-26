package com.jvfault.config;

/**
 * 配置模块异常。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class ConfigException extends RuntimeException {

    private final String key;

    public ConfigException(String message) {
        super(message);
        this.key = null;
    }

    public ConfigException(String message, String key) {
        super(message + (key != null ? " (key: " + key + ")" : ""));
        this.key = key;
    }

    public ConfigException(String message, String key, Throwable cause) {
        super(message + (key != null ? " (key: " + key + ")" : ""), cause);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
