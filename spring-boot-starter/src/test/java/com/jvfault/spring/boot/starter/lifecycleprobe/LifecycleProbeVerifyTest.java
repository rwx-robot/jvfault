package com.jvfault.spring.boot.starter.lifecycleprobe;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import com.jvfault.spring.boot.starter.JvfaultAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 临时验证：FactoryBean 桥接后 jvfault 自有 bean 的生命周期归 jvfault 独占。
 * 验证完毕后删除。
 */
@DisplayName("FactoryBean 桥接生命周期验证（临时）")
class LifecycleProbeVerifyTest {

    private static final String PKG = "com.jvfault.spring.boot.starter.lifecycleprobe";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvfaultAutoConfiguration.class));

    @BeforeEach
    void reset() {
        SingletonProbe.reset();
        PrototypeProbe.reset();
    }

    @Test
    @DisplayName("@PostConstruct 恰好 1 次，且类型注入可用，Spring 侧拿到的是 jvfault 同一实例")
    void postConstructOnceAndSameInstance() {
        runner.withPropertyValues("jvfault.base-packages=" + PKG).run(context -> {
            ModuleContainer container = context.getBean(ModuleContainer.class);
            BeanRegistry registry = container.getBeanRegistry();

            SingletonProbe fromSpring = context.getBean(SingletonProbe.class);
            assertNotNull(fromSpring);
            assertSame(registry.getBean(SingletonProbe.class), fromSpring,
                    "Spring 侧拿到的必须是 jvfault 容器里的同一实例");
            assertEquals(1, SingletonProbe.constructCount,
                    "@PostConstruct 应只被调用 1 次，实测 " + SingletonProbe.constructCount);
        });
    }

    @Test
    @DisplayName("关闭上下文后 @PreDestroy 恰好 1 次（不被 Spring 与 jvfault 各调一次）")
    void preDestroyOnceOnClose() {
        runner.withPropertyValues("jvfault.base-packages=" + PKG).run(context -> {
            assertNotNull(context.getBean(SingletonProbe.class));
            assertEquals(0, SingletonProbe.destroyCount, "关闭前不应被销毁");
        });
        assertEquals(1, SingletonProbe.destroyCount,
                "@PreDestroy 应只被调用 1 次，实测 " + SingletonProbe.destroyCount);
    }

    @Test
    @DisplayName("jvfault PROTOTYPE 组件暴露到 Spring 后每次取到不同实例（未被压平成单例）")
    void prototypeNotFlattened() {
        runner.withPropertyValues("jvfault.base-packages=" + PKG).run(context -> {
            PrototypeProbe first = context.getBean(PrototypeProbe.class);
            PrototypeProbe second = context.getBean(PrototypeProbe.class);
            assertNotSame(first, second, "prototype 组件在 Spring 侧应每次取到新实例");
        });
    }
}
