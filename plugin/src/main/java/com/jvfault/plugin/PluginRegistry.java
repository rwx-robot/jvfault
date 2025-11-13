package com.jvfault.plugin;

import java.util.Optional;

/**
 * 插件注册表 - 供 PluginContext 查询其他插件的服务。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class PluginRegistry {

    private final PluginManager manager;

    PluginRegistry(PluginManager manager) {
        this.manager = manager;
    }

    public <T> Optional<T> getService(String pluginId, Class<T> type) {
        PluginManager.Loaded loaded = manager.get(pluginId);
        if (loaded != null && type.isInstance(loaded.plugin)) {
            return Optional.of(type.cast(loaded.plugin));
        }
        return Optional.empty();
    }
}
