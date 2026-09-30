package com.jvfault.spring.boot.starter;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.spring.boot.starter.fixture.ClockService;
import com.jvfault.spring.boot.starter.reversefixture.TimeAwareGreeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 反向注入（Spring → jvfault）的<b>边界</b>回归。
 *
 * <p>与 {@link JvfaultAutoConfigurationTest} 的分工：那边验证「正常路径能通」，
 * 这边专门盯<b>退化路径与重复注册</b> —— 这些路径一旦静默失效，
 * 现象是「注入到错误的实例 / 注入不到」，排查成本极高。
 *
 * <p><b>为什么用 {@code withInitializer} + {@code registerSingleton} 而不是 {@code withBean}</b>：
 * {@code withBean} 底层一律生成 {@code BeanDefinition}，<b>结构上测不到</b>
 * 「手工注册单例（无 BeanDefinition）」这条分支 —— 而这条分支正是
 * {@code getBeanDefinition} 会抛 {@code NoSuchBeanDefinitionException} 的场景。
 *
 * @since v1.0.14 (2026)
 */
@DisplayName("反向注入边界：手工单例 / lazy-init / prototype / 自定义 bean 名")
class ReverseInjectionEdgeCaseTest {

    private static final String FIXTURE_PKG = "com.jvfault.spring.boot.starter.fixture";
    private static final String REVERSE_PKG = "com.jvfault.spring.boot.starter.reversefixture";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvfaultAutoConfiguration.class));

    @BeforeEach
    void resetCounters() {
        CountingClockService.reset();
    }

    @Test
    @DisplayName("手工注册单例（无 BeanDefinition）：仍被反向导入，且不产生重复 Spring bean")
    void manuallyRegisteredSingletonIsImported() {
        runner.withPropertyValues(
                        "jvfault.base-packages=" + REVERSE_PKG,
                        "jvfault.import-spring-beans=true")
                // 手工单例走 registerSingleton —— 正是 withBean 覆盖不到的分支
                .withInitializer(ctx -> ctx.getBeanFactory().registerSingleton("clock", new ClockService()))
                .run(context -> {
                    assertThat(context.getStartupFailure())
                            .as("手工单例没有 BeanDefinition，getBeanDefinition 会抛异常；这里必须仍能启动")
                            .isNull();
                    // 证明本用例确实站在「手工单例」这条分支上：
                    // Spring 侧有它的实例、却没有它的 BeanDefinition
                    assertThat(context.getBeanDefinitionNames()).doesNotContain("clock");

                    BeanRegistry registry = context.getBean(BeanRegistry.class);
                    assertThat(registry.containsBean("clock")).isTrue();
                    assertThat(registry.getBean(ClockService.class)).isNotNull();

                    // 反向注入真的生效：jvfault 组件拿到了它
                    assertThat(context.getBean(TimeAwareGreeter.class).greet("ny"))
                            .startsWith("hello ny @ ");

                    // 双向同开时不得把已导入的 bean 又倒回 Spring（否则同一实例两个身份）
                    assertThat(context.getBeanNamesForType(ClockService.class))
                            .containsExactly("clock");
                });
    }

    @Test
    @DisplayName("lazy-init 的 Spring bean：跳过导入，且<b>不</b>被强制提前实例化")
    void lazyInitSpringBeanIsSkippedAndNotInstantiated() {
        // 对照组：同一个类、非 lazy-init 时**会**被导入 —— 证明下面的「跳过」是 lazy 造成的，
        // 而不是「这个类压根没被 @JvfaultComponent 发现」导致的假绿。
        runner.withPropertyValues(
                        "jvfault.base-packages=" + FIXTURE_PKG,
                        "jvfault.import-spring-beans=true")
                .withBean("clock", CountingClockService.class, CountingClockService::new)
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context.getBean(BeanRegistry.class).containsBean(ClockService.class)).isTrue();
                    assertThat(CountingClockService.created()).isPositive();
                });

        CountingClockService.reset();
        runner.withPropertyValues(
                        "jvfault.base-packages=" + FIXTURE_PKG,
                        "jvfault.import-spring-beans=true")
                .withBean("clock", CountingClockService.class, CountingClockService::new,
                        bd -> bd.setLazyInit(true))
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();

                    BeanRegistry registry = context.getBean(BeanRegistry.class);
                    assertThat(registry.containsBean(ClockService.class))
                            .as("lazy-init bean 不应进入 jvfault 容器")
                            .isFalse();
                    assertThat(CountingClockService.created())
                            .as("反向注入不得破坏 @Lazy 语义：整个启动过程都不该实例化它")
                            .isZero();
                });
    }

    @Test
    @DisplayName("prototype 作用域的 Spring bean：跳过导入，容器正常启动")
    void prototypeSpringBeanIsSkipped() {
        runner.withPropertyValues(
                        "jvfault.base-packages=" + FIXTURE_PKG,
                        "jvfault.import-spring-beans=true")
                .withBean("clock", ClockService.class, ClockService::new,
                        bd -> bd.setScope(BeanDefinition.SCOPE_PROTOTYPE))
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context.getBean(BeanRegistry.class).containsBean(ClockService.class))
                            .as("非 singleton 作用域 jvfault 无法托管，应跳过而不是强行导入")
                            .isFalse();
                });
    }

    @Test
    @DisplayName("自定义 Spring bean 名（≠ jvfault 默认名）：两个容器各只有 1 个 ClockService")
    void customSpringBeanNameDoesNotDuplicateSingleton() {
        runner.withPropertyValues(
                        "jvfault.base-packages=" + REVERSE_PKG,
                        "jvfault.import-spring-beans=true")
                .withUserConfiguration(CustomNamedClockConfig.class)
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();

                    // Spring 侧：只能是「用户自己声明的那一个」
                    assertThat(context.getBeanNamesForType(ClockService.class))
                            .containsExactly("systemClock");
                    assertThat(context.getBean(ClockService.class))
                            .isSameAs(context.getBean("systemClock"));

                    // jvfault 侧：沿用 Spring 的 bean 名，一一对应便于排查
                    assertThat(context.getBean(BeanRegistry.class).containsBean("systemClock")).isTrue();
                    assertThat(context.getBean(TimeAwareGreeter.class).greet("ny"))
                            .startsWith("hello ny @ ");
                });
    }

    /** 自定义 bean 名：与 jvfault 的「简单名首字母小写」不一致，最容易暴露重复注册。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomNamedClockConfig {

        @Bean
        ClockService systemClock() {
            return new ClockService();
        }
    }

    /** 带构造计数器的时钟：用来证明「没有被提前实例化」。 */
    @JvfaultComponent
    public static class CountingClockService extends ClockService {

        private static final AtomicInteger CREATED = new AtomicInteger();

        public CountingClockService() {
            CREATED.incrementAndGet();
        }

        static int created() {
            return CREATED.get();
        }

        static void reset() {
            CREATED.set(0);
        }
    }
}
