package com.jvfault.example.springbridge;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 以 {@code web=NONE} 启动整个示例应用（不起 Tomcat，CI 友好），端到端验证双向桥接：
 * <ol>
 *   <li>正向：jvfault 的 {@link Greeter} 暴露为 Spring bean，可从 Spring 取得并调用；</li>
 *   <li>反向：Spring 的 {@link ClockService} 被注册进 jvfault 容器，
 *       {@code Greeter} 通过 {@code @Inject} 使用它 —— 且是<b>同一个实例</b>。</li>
 * </ol>
 *
 * @since v1.0.14 (2026)
 */
@DisplayName("Spring Boot ↔ jvfault 双向桥接示例")
class SpringBridgeExampleTest {

    @Test
    @DisplayName("启动后：Greeter 可从 Spring 取得；ClockService 反向注入 jvfault 且为同一实例")
    void bridgeWorksBothDirections() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ExampleApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {

            // 正向：jvfault bean 暴露给 Spring
            Greeter greeter = ctx.getBean(Greeter.class);
            String greeting = greeter.greet("ny");
            assertThat(greeting).startsWith("hi ny").contains("bridged at");

            // 反向：Spring bean 进入 jvfault 容器，且复用同一单例
            ModuleContainer container = ctx.getBean(ModuleContainer.class);
            BeanRegistry registry = container.getBeanRegistry();
            ClockService inJvfault = registry.getBean(ClockService.class);
            assertThat(inJvfault).isSameAs(ctx.getBean(ClockService.class));
        }
    }
}
