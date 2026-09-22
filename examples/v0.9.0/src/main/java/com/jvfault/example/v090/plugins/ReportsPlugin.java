package com.jvfault.example.v090.plugins;

import com.jvfault.plugin.Plugin;
import com.jvfault.plugin.PluginContext;

import java.util.Collections;
import java.util.List;

/**
 * 报表插件 —— 依赖 metrics 插件，验证依赖拓扑顺序启动。
 *
 * @since v0.9.0 (2023)
 */
public class ReportsPlugin implements Plugin {

    private PluginContext context;

    @Override
    public String getId() {
        return "reports";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> dependencies() {
        return Collections.singletonList("metrics");
    }

    @Override
    public void init(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start() {
        context.logger().info("reports 启动（依赖 metrics 已就绪）");
    }

    @Override
    public void stop() {
        context.logger().info("reports 停止");
    }
}
