package com.jvfault.example.v090.plugins;

import com.jvfault.plugin.Plugin;
import com.jvfault.plugin.PluginContext;

import java.util.Collections;
import java.util.List;

/**
 * 指标插件 —— 无依赖，最先启动。
 *
 * @since v0.9.0 (2023)
 */
public class MetricsPlugin implements Plugin {

    private PluginContext context;

    @Override
    public String getId() {
        return "metrics";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> dependencies() {
        return Collections.emptyList();
    }

    @Override
    public void init(PluginContext context) {
        this.context = context;
        context.logger().info("metrics 初始化: env={}", context.getConfig("env", "dev"));
    }

    @Override
    public void start() {
        context.setConfig("metrics.endpoint", "/actuator/metrics");
        context.logger().info("metrics 启动，暴露 {}", context.getConfig("metrics.endpoint", "?"));
    }

    @Override
    public void stop() {
        context.logger().info("metrics 停止");
    }
}
