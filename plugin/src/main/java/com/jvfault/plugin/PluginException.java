package com.jvfault.plugin;

/**
 * 插件异常。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class PluginException extends RuntimeException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
