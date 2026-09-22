package com.jvfault.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 插件运行时上下文。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class PluginContext {

    private final String pluginId;
    private final ClassLoader pluginClassLoader;
    private final java.util.concurrent.ConcurrentHashMap<String, String> config;
    private final PluginRegistry registry;

    public PluginContext(String pluginId, ClassLoader pluginClassLoader,
                         java.util.Map<String, String> config, PluginRegistry registry) {
        this.pluginId = pluginId;
        this.pluginClassLoader = pluginClassLoader;
        this.config = new java.util.concurrent.ConcurrentHashMap<>(config);
        this.registry = registry;
    }

    public String getPluginId() {
        return pluginId;
    }

    public ClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }

    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public void setConfig(String key, String value) {
        config.put(key, value);
    }

    /** 获取已启动插件提供的服务 */
    public <T> Optional<T> getService(String pluginId, Class<T> type) {
        return registry.getService(pluginId, type);
    }

    public Logger logger() {
        return LoggerFactory.getLogger("jvfault.plugin." + pluginId);
    }
}
