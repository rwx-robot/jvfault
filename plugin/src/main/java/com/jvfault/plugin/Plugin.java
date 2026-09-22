package com.jvfault.plugin;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * 插件 SPI - 框架进程内的可隔离加载扩展单元。
 *
 * <p>实现类通过 ServiceLoader 发现：
 * {@code META-INF/services/com.jvfault.plugin.Plugin}。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public interface Plugin {

    String getId();

    String getVersion();

    /** 依赖的其他插件 id */
    default List<String> dependencies() {
        return Collections.emptyList();
    }

    void init(PluginContext context) throws Exception;

    void start() throws Exception;

    void stop() throws Exception;
}
