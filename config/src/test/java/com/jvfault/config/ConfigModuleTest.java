package com.jvfault.config;

import com.jvfault.config.annotation.ConfigurationProperties;
import com.jvfault.config.annotation.Profile;
import com.jvfault.config.annotation.Value;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Module;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-config 核心测试
 *
 * @since v0.3.0 (2017)
 */
@DisplayName("Config 模块测试")
class ConfigModuleTest {

    // ============ 属性源与解析 ============

    @Test
    @DisplayName("YAML 加载与嵌套扁平化")
    void testYamlFlattening() {
        String yaml = "server:\n  host: localhost\n  port: 8080\n";
        YamlPropertySource source = new YamlPropertySource("test",
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("localhost", source.getProperty("server.host"));
        assertEquals("8080", source.getProperty("server.port"));
        assertNull(source.getProperty("server.missing"));
    }

    @Test
    @DisplayName("JSON 加载与扁平化")
    void testJsonFlattening() {
        String json = "{\"a\": {\"b\": {\"c\": \"deep\"}}, \"n\": 3}";
        JsonPropertySource source = new JsonPropertySource("test",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertEquals("deep", source.getProperty("a.b.c"));
        assertEquals("3", source.getProperty("n"));
    }

    @Test
    @DisplayName("属性解析按注册顺序优先")
    void testSourceOrdering() {
        ConfigurableEnvironment env = new ConfigurableEnvironment();
        // 语义: 先注册者优先（与 ConfigLoader 的 addFirst 覆盖链一致）
        env.addPropertySource(new MapPropertySource("high", PropertyFlattener.flatten(
                java.util.Collections.<String, Object>singletonMap("key", "high"))));
        env.addPropertySource(new MapPropertySource("low", PropertyFlattener.flatten(
                java.util.Collections.<String, Object>singletonMap("key", "low"))));

        assertEquals("high", env.getProperty("key"));
    }

    @Test
    @DisplayName("类型转换与 getRequiredProperty")
    void testTypedAccess() {
        ConfigurableEnvironment env = new ConfigurableEnvironment();
        env.addPropertySource(new MapPropertySource("m", PropertyFlattener.flatten(
                new java.util.LinkedHashMap<String, Object>() {{
                    put("port", "9090");
                    put("debug", "true");
                    put("mode", "FAST");
                    put("badnum", "abc");
                }})));

        assertEquals(9090, env.getInt("port", 0));
        assertTrue(env.getBoolean("debug", false));
        assertEquals(Mode.FAST, env.<Mode>get("mode", Mode.class));
        assertEquals("x", env.getProperty("missing", "x"));
        assertThrows(ConfigException.class, () -> env.getRequiredProperty("missing"));
        assertThrows(ConfigException.class, () -> env.getInt("badnum", 0));
    }

    enum Mode { FAST, SLOW }

    @Test
    @DisplayName("resolvePlaceholders 解析与默认值")
    void testPlaceholders() {
        ConfigurableEnvironment env = new ConfigurableEnvironment();
        env.addPropertySource(new MapPropertySource("m", PropertyFlattener.flatten(
                java.util.Collections.<String, Object>singletonMap("base.url", "http://api"))));

        assertEquals("http://api/v1", env.resolvePlaceholders("${base.url}/v1"));
        assertEquals("fallback", env.resolvePlaceholders("${missing.key:fallback}"));
        assertEquals("${no.default}", env.resolvePlaceholders("${no.default}"));
        assertEquals("plain", env.resolvePlaceholders("plain"));
    }

    // ============ 配置链 ============

    @Test
    @DisplayName("多配置源 classpath 加载（yml/properties/json 共存）")
    void testClasspathSources() {
        ConfigurableEnvironment env = ConfigLoader.load();
        assertNotNull(env.getProperty("app.name"));        // yml
        assertEquals("from-properties", env.getProperty("legacy.key")); // properties
        assertEquals("jsonValue", env.getProperty("jsonKey.nested"));   // json
    }

    // ============ 容器集成（profile=prod） ============

    private static String savedProfiles;

    @BeforeAll
    static void activateProdProfile() {
        savedProfiles = System.getProperty(DefaultConfigEnvironment.PROFILES_PROPERTY);
        System.setProperty(DefaultConfigEnvironment.PROFILES_PROPERTY, "prod");
    }

    @AfterAll
    static void restoreProfiles() {
        if (savedProfiles != null) {
            System.setProperty(DefaultConfigEnvironment.PROFILES_PROPERTY, savedProfiles);
        } else {
            System.clearProperty(DefaultConfigEnvironment.PROFILES_PROPERTY);
        }
    }

    @ConfigurationProperties("app")
    static class AppConfig {
        String name;
        int timeout;
        List<String> tags;
        DbConfig db;

        static class DbConfig {
            String url;
            int poolSize;
        }
    }

    @Component
    static class BoundService {
        @Value("${server.port:0}")
        int port;

        @Value("app.name")
        String appName;
    }

    @Component
    @Profile("prod")
    static class ProdOnlyService {
    }

    @Module(imports = ConfigModule.class, providers = {
            BoundService.class, ProdOnlyService.class, AppConfig.class})
    static class TestAppModule {
    }

    @Test
    @DisplayName("容器集成: profile 覆盖 + @Value + @ConfigurationProperties")
    void testContainerIntegration() {
        com.jvfault.core.module.ModuleContainer container =
                JvfaultApplication.createContainer(TestAppModule.class);
        try {
            // profile 覆盖: prod 的 timeout=60 覆盖基础 30
            BoundService bound = container.getBeanRegistry().getBean(BoundService.class);
            assertEquals(9090, bound.port);
            assertEquals("jvfault-prod", bound.appName);

            AppConfig config = container.getBeanRegistry().getBean(AppConfig.class);
            assertEquals(60, config.timeout);
            assertEquals("jvfault-prod", config.name);
            assertEquals(java.util.Arrays.asList("alpha", "beta"), config.tags);
            assertEquals("jdbc:h2:mem:test", config.db.url);
            assertEquals(5, config.db.poolSize);
        } finally {
            container.destroy();
        }
    }

    @Test
    @DisplayName("@Profile 不匹配时 fail-fast")
    void testProfileMismatch() {
        @Module(imports = ConfigModule.class, providers = {DevOnlyService.class})
        class DevModule {
        }
        assertThrows(RuntimeException.class,
                () -> JvfaultApplication.createContainer(DevModule.class));
    }

    @Component
    @Profile("dev")
    static class DevOnlyService {
    }
}
