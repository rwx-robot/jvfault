package com.jvfault.spring.boot.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jvfault 桥接配置（前缀 {@code jvfault}）。
 *
 * <p><b>必须显式设置 {@code jvfault.base-packages}</b> 才会激活自动配置 ——
 * 这是刻意的 opt-in 设计：把 starter 放进依赖树不会有任何副作用，
 * 直到你声明要扫描哪些包。
 *
 * @since v1.0.11 (2026)
 */
@ConfigurationProperties(prefix = "jvfault")
public class JvfaultProperties {

    /** 要扫描的基础包（逗号分隔或 YAML 列表）。 */
    private String[] basePackages = new String[0];

    /**
     * 根模块类的全限定名。留空时自动推断：在 basePackages 下找<b>唯一</b>的
     * {@code @Module} 类；找不到或有多个则启动失败并给出明确提示。
     */
    private String rootModule;

    /** 是否把 jvfault 容器里的 bean 暴露为 Spring bean（默认开）。 */
    private boolean exposeBeans = true;

    public String[] getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    public String getRootModule() {
        return rootModule;
    }

    public void setRootModule(String rootModule) {
        this.rootModule = rootModule;
    }

    public boolean isExposeBeans() {
        return exposeBeans;
    }

    public void setExposeBeans(boolean exposeBeans) {
        this.exposeBeans = exposeBeans;
    }
}
