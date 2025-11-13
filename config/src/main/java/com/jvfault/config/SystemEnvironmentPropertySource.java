package com.jvfault.config;

import java.util.Locale;

/**
 * 系统环境变量属性源，key 宽松匹配：a.b.c -> A_B_C。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
public class SystemEnvironmentPropertySource extends PropertySource {

    public SystemEnvironmentPropertySource() {
        super("systemEnvironment");
    }

    @Override
    public String getProperty(String key) {
        String direct = System.getenv(key);
        if (direct != null) {
            return direct;
        }
        String relaxed = key.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        return System.getenv(relaxed);
    }
}
