package com.jvfault.config;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;
import com.jvfault.core.annotation.Module;

/**
 * 配置模块 - 一站式装配配置基础设施。
 *
 * <pre>{@code
 * @Module(imports = ConfigModule.class, providers = {MyService.class})
 * public class AppModule {}
 * }</pre>
 *
 * <p>装配链：BPP 最先创建 → 构造器注入 DefaultConfigEnvironment（构造时
 * 加载配置链）→ 后续 Bean 在初始化前完成 @Value / @ConfigurationProperties
 * 注入与 @Profile 校验。
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Module(providers = {DefaultConfigEnvironment.class, ConfigBeanPostProcessor.class})
public class ConfigModule {
}
