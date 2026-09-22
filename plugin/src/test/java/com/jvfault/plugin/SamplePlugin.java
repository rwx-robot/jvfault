package com.jvfault.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试插件实现（ServiceLoader 注册，见 META-INF/services）。
 */
public class SamplePlugin implements Plugin {

    public static final List<String> CALLS = new ArrayList<>();

    private PluginContext context;

    @Override
    public String getId() {
        return "sample";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void init(PluginContext context) {
        this.context = context;
        CALLS.add("sample.init:" + context.getConfig("greeting", "none"));
    }

    @Override
    public void start() {
        CALLS.add("sample.start");
    }

    @Override
    public void stop() {
        CALLS.add("sample.stop");
    }

    public PluginContext getContext() {
        return context;
    }
}
