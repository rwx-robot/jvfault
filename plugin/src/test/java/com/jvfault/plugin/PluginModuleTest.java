package com.jvfault.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-plugin 核心测试
 *
 * @since v0.9.0 (2023)
 */
@DisplayName("Plugin 模块测试")
class PluginModuleTest {

    private PluginManager manager;

    @BeforeEach
    void setUp() {
        SamplePlugin.CALLS.clear();
        Map<String, String> config = new HashMap<>();
        config.put("greeting", "hi");
        manager = new PluginManager(config, true);
    }

    @AfterEach
    void tearDown() {
        SamplePlugin.CALLS.clear();
    }

    @Test
    @DisplayName("classpath ServiceLoader 发现并按状态机启动")
    void testClasspathDiscoveryAndLifecycle() {
        int count = manager.loadFromClasspath();
        assertEquals(1, count);

        PluginManager.Loaded loaded = manager.get("sample");
        assertEquals(PluginManager.State.CREATED, loaded.state);

        manager.startAll();
        assertEquals(PluginManager.State.STARTED, loaded.state);
        assertEquals("sample.init:hi", SamplePlugin.CALLS.get(0));
        assertEquals("sample.start", SamplePlugin.CALLS.get(1));

        manager.stop("sample");
        assertEquals(PluginManager.State.STOPPED, loaded.state);
        assertEquals("sample.stop", SamplePlugin.CALLS.get(2));
    }

    @Test
    @DisplayName("非法状态迁移抛异常")
    void testIllegalTransition() {
        manager.loadFromClasspath();
        assertThrows(PluginException.class, () -> manager.stop("sample"), "CREATED 不可直接 stop");

        manager.start("sample");
        assertThrows(PluginException.class, () -> manager.start("sample"), "重复 start");
    }

    @Test
    @DisplayName("未加载插件操作抛异常")
    void testUnknownPlugin() {
        assertThrows(PluginException.class, () -> manager.start("ghost"));
    }

    @Test
    @DisplayName("context 配置读写与服务查询")
    void testContext() {
        manager.loadFromClasspath();
        PluginManager.Loaded loaded = manager.get("sample");
        manager.start("sample");

        assertEquals("hi", loaded.context.getConfig("greeting", "def"));
        assertEquals("def", loaded.context.getConfig("missing", "def"));
        loaded.context.setConfig("runtime", "value");
        assertEquals("value", loaded.context.getConfig("runtime", "def"));

        assertTrue(loaded.context.getService("sample", SamplePlugin.class).isPresent());
        assertFalse(loaded.context.getService("sample", String.class).isPresent());
    }
}
