package com.jvfault.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置加载器 - 从 classpath / 文件加载配置并装配 Environment。
 *
 * <p>约定：
 * <ul>
 *   <li>classpath 根下 application.yml / application.properties / application.json 自动探测</li>
 *   <li>每个 activeProfile 加载 application-{profile}.yml（优先级更高，插到队首）</li>
 *   <li>系统属性与环境变量源默认加入（优先级最高）</li>
 * </ul>
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class ConfigLoader {

    /**
     * 加载默认配置链：application.* + profile 覆盖 + 系统属性 + 环境变量。
     */
    public static ConfigurableEnvironment load(String... profiles) {
        ConfigurableEnvironment env = new ConfigurableEnvironment();
        env.setActiveProfiles(profiles);

        // 基础配置（最低优先级，先注册；多格式共存，先注册者优先）
        for (String ext : new String[]{"yml", "properties", "json"}) {
            InputStream in = ConfigLoader.class.getResourceAsStream("/application." + ext);
            if (in != null) {
                env.addPropertySource(createSource("application." + ext, ext, in));
            }
        }

        // profile 覆盖（更高优先级，逆序插入保证 profile 列表靠前者优先）
        List<String> profileList = new ArrayList<>(java.util.Arrays.asList(profiles));
        java.util.Collections.reverse(profileList);
        for (String profile : profileList) {
            for (String ext : new String[]{"yml", "properties", "json"}) {
                InputStream in = ConfigLoader.class.getResourceAsStream("/application-" + profile + "." + ext);
                if (in != null) {
                    env.addFirst(createSource("application-" + profile + "." + ext, ext, in));
                }
            }
        }

        // 系统属性 / 环境变量（最高优先级）
        env.addFirst(new SystemEnvironmentPropertySource());
        env.addFirst(new SystemPropertiesPropertySource());
        return env;
    }

    private static PropertySource createSource(String name, String ext, InputStream in) {
        if ("yml".equals(ext)) {
            return new YamlPropertySource(name, in);
        }
        if ("json".equals(ext)) {
            return new JsonPropertySource(name, in);
        }
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(in);
        } catch (Exception e) {
            throw new ConfigException("properties 配置解析失败: " + name, null, e);
        }
        return new PropertiesPropertySource(name, props);
    }
}
