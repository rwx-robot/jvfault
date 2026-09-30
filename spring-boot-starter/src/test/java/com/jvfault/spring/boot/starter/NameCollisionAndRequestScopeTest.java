package com.jvfault.spring.boot.starter;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.spring.boot.starter.collidefixture.Greeter;
import com.jvfault.spring.boot.starter.requestfixture.RequestProbe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨容器<b>撞名</b>与<b>作用域</b>回归。
 *
 * <p>两条用例都是「钉现状」：实测行为与框架的宣称不符，这里先把<b>实际行为</b>锁住，
 * 让任何一次行为变化（无论变好还是变坏）都必须显式改测试 —— 而不是让它在文档里继续漂移。
 *
 * @since v1.0.14 (2026)
 */
@DisplayName("跨容器撞名与作用域")
class NameCollisionAndRequestScopeTest {

    private static final String COLLIDE_PKG = "com.jvfault.spring.boot.starter.collidefixture";
    private static final String REQUEST_PKG = "com.jvfault.spring.boot.starter.requestfixture";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvfaultAutoConfiguration.class));

    /**
     * <b>钉现状（与宣称不符，见 TODO）</b>：期望行为是「启动失败并明确报错」，
     * 实测是<b>静默成功</b> —— jvfault 自有的 {@code Greeter} 被丢掉，容器里只剩 Spring 的实例。
     *
     * <p>根因：撞名发生在 refresh 阶段（jvfault 组件登记时），而 starter 的撞名自检只覆盖
     * 反向导入阶段；core 侧 {@code ModuleContainer.registerProviderClass} 对同名走
     * {@code log.debug("already registered by global module, skipping")} 静默跳过，
     * 连日志都谎报了原因（并非 global module 注册）。
     *
     * <p>修复后请把下面的断言改成「{@code getStartupFailure() != null} 且消息含 greeter」。
     */
    @Test
    @DisplayName("撞名（钉现状，与宣称不符，见 TODO）：jvfault 自有 @Component 被静默丢弃，Spring 实例胜出")
    void nameCollisionCurrentlySilentlyDropsJvfaultComponent() {
        runner.withPropertyValues(
                        "jvfault.base-packages=" + COLLIDE_PKG,
                        "jvfault.import-spring-beans=true")
                .withBean("greeter", SpringGreeter.class, SpringGreeter::new)
                .run(context -> {
                    // 钉现状：这里<b>不该</b>启动成功
                    assertThat(context.getStartupFailure())
                            .as("钉现状：撞名当前是静默成功（与宣称不符，见 TODO）")
                            .isNull();

                    BeanRegistry registry = context.getBean(BeanRegistry.class);
                    assertThat(registry.getBean("greeter"))
                            .as("同名 bean 现在只剩 Spring 侧的实例")
                            .isInstanceOf(SpringGreeter.class);
                    assertThat(registry.containsBean(Greeter.class))
                            .as("jvfault 自有的 Greeter 被静默丢掉，容器里根本没有它")
                            .isFalse();
                });
    }

    @Test
    @DisplayName("REQUEST 作用域（钉现状，与宣称不符，见 TODO）：两次 enterRequestScope 取到同一实例")
    void requestScopedBeanIsCurrentlySharedAcrossRequestScopes() {
        runner.withPropertyValues("jvfault.base-packages=" + REQUEST_PKG)
                .run(context -> {
                    BeanRegistry registry = context.getBean(BeanRegistry.class);

                    Object firstScope = registry.enterRequestScope();
                    RequestProbe first = context.getBean(RequestProbe.class);
                    registry.exitRequestScope(firstScope);

                    Object secondScope = registry.enterRequestScope();
                    RequestProbe second = context.getBean(RequestProbe.class);
                    registry.exitRequestScope(secondScope);

                    // 钉现状：REQUEST 作用域目前退化成单例 ——
                    // ① core 的 getBeanInstance 把 REQUEST 与 SINGLETON 同等处理并缓存；
                    // ② 暴露到 Spring 时 isSingleton() = !isPrototype(name)，REQUEST 也判为 true。
                    // 与「每个请求一个实例」的宣称不符，修复前请勿把它读成正确行为。
                    assertThat(second)
                            .as("钉现状：REQUEST 作用域当前跨请求复用同一实例（与宣称不符，见 TODO）")
                            .isSameAs(first);
                });
    }

    /** Spring 侧同名 bean：类型与 jvfault 的 Greeter 不同，只有<b>名字</b>撞上。 */
    @JvfaultComponent
    public static class SpringGreeter {

        public String greet() {
            return "hi from spring";
        }
    }
}
