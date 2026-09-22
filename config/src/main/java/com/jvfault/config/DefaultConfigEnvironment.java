package com.jvfault.config;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;

/**
 * 默认 Environment Bean。
 *
 * <p>加载顺序：classpath application.{yml,properties,json} →
 * application-{profile}.* → 系统属性 → 环境变量。
 * Profile 通过系统属性 {@code jvfault.profiles}（逗号分隔）指定。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Component
public class DefaultConfigEnvironment extends ConfigurableEnvironment {

    public static final String PROFILES_PROPERTY = "jvfault.profiles";

    @Inject
    public DefaultConfigEnvironment() {
        String raw = System.getProperty(PROFILES_PROPERTY, "");
        if (!raw.trim().isEmpty()) {
            String[] profiles = raw.trim().split("\\s*,\\s*");
            setActiveProfiles(profiles);
            addAll(ConfigLoader.load(profiles));
        } else {
            addAll(ConfigLoader.load());
        }
    }

    /** 复制 loader 装配好的属性源 */
    private void addAll(ConfigurableEnvironment env) {
        for (PropertySource source : env.getPropertySources()) {
            addPropertySource(source);
        }
        for (String p : env.getActiveProfiles()) {
            addActiveProfile(p);
        }
    }
}
