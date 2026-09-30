package com.jvfault.spring.boot.starter;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import com.jvfault.spring.boot.starter.fixture.ClockService;
import com.jvfault.spring.boot.starter.fixture.GreetingService;
import com.jvfault.spring.boot.starter.reversefixture.ManualClock;
import com.jvfault.spring.boot.starter.reversefixture.TimeAwareGreeter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot 自动配置桥接回归。
 *
 * <p>覆盖两件事：
 * <ol>
 *   <li>声明 {@code jvfault.base-packages} 后容器能被创建，且 jvfault 的 bean
 *       能被 Spring 直接按类型注入（jvfault → Spring 单向暴露）。</li>
 *   <li>不声明该属性时<b>完全不激活</b> —— 把 starter 放进依赖树没有副作用（opt-in）。</li>
 * </ol>
 *
 * @since v1.0.11 (2026)
 */
@DisplayName("Spring Boot starter 自动配置桥接")
class JvfaultAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvfaultAutoConfiguration.class));

    @Test
    @DisplayName("声明 base-packages 后：容器创建成功，且 jvfault bean 可从 Spring 取得")
    void containerCreatedAndBeansExposedToSpring() {
        runner.withPropertyValues("jvfault.base-packages=com.jvfault.spring.boot.starter.fixture")
                .run(context -> {
                    assertNotNull(context.getBean(ModuleContainer.class),
                            "ModuleContainer 应被注册为 Spring bean");
                    assertNotNull(context.getBean(BeanRegistry.class),
                            "BeanRegistry 应被注册为 Spring bean");

                    // 核心断言：jvfault 容器里的组件能被 Spring 按类型拿到
                    GreetingService service = context.getBean(GreetingService.class);
                    assertEquals("hello-jvfault", service.greet());
                });
    }

    @Test
    @DisplayName("不声明 base-packages 时：不激活，容器 bean 不存在（opt-in）")
    void notActivatedWithoutBasePackages() {
        runner.run(context ->
                assertEquals(0, context.getBeansOfType(ModuleContainer.class).size(),
                        "未配置 jvfault.base-packages 时不应创建容器"));
    }

    @Test
    @DisplayName("import-spring-beans=true：标注 @JvfaultComponent 的 Spring bean 被反向注入，jvfault @Component 可 @Inject 它")
    void springBeansImportedIntoJvfault() {
        runner
                .withPropertyValues(
                        "jvfault.base-packages=com.jvfault.spring.boot.starter.reversefixture",
                        "jvfault.import-spring-beans=true")
                // 这是 Spring 侧被 @JvfaultComponent 标注的 bean，会被反向注入 jvfault
                .withBean(ClockService.class)
                .run(context -> {
                    ModuleContainer container = context.getBean(ModuleContainer.class);
                    BeanRegistry registry = context.getBean(BeanRegistry.class);

                    // 反向注入：ClockService 出现在 jvfault 容器里
                    ClockService imported = registry.getBean(ClockService.class);
                    assertNotNull(imported,
                            "标注 @JvfaultComponent 的 Spring bean 应被注册进 jvfault 容器");

                    // jvfault @Component 通过 @Inject 拿到了 Spring bean，且能真正调用
                    TimeAwareGreeter greeter = context.getBean(TimeAwareGreeter.class);
                    String greeting = greeter.greet("ny");
                    assertTrue(greeting.startsWith("hello ny @ "),
                            "greeter 应使用注入的 ClockService 生成时间戳，实际：" + greeting);

                    // 同一个实例：Spring 里的 ClockService 与 jvfault 里的是同一个
                    assertSame(context.getBean(ClockService.class), imported,
                            "反向注入应复用 Spring 原有的单例实例，而非新建");
                });
    }

    @Test
    @DisplayName("import-spring-beans 缺省为 false：即使存在 @JvfaultComponent 的 Spring bean，也不会进入 jvfault")
    void reverseInjectionOffByDefault() {
        runner
                .withPropertyValues("jvfault.base-packages=com.jvfault.spring.boot.starter.fixture")
                .withBean(ClockService.class)
                .run(context -> {
                    ModuleContainer container = context.getBean(ModuleContainer.class);
                    BeanRegistry registry = context.getBean(BeanRegistry.class);
                    assertFalse(registry.containsBean(ClockService.class),
                            "import-spring-beans 缺省关闭，Spring bean 不应进入 jvfault 容器");
                });
    }
}
