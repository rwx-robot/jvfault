package com.jvfault.example.springbridge;

import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring 控制器：注入 jvfault 的 {@link Greeter}（正向暴露）。
 * 一次调用串起两个容器：Spring 请求 → jvfault Greeter → Spring ClockService → 返回。
 *
 * <p><b>为什么需要 {@code @DependsOn("jvfaultModuleContainer")}</b>（实测，勿删）：
 * jvfault 的 bean 要等自动配置的 {@code @Bean} <b>执行时</b>才登记进 Spring，
 * 而用户 bean（本控制器）在刷新早期就<b>先于</b>自动配置 bean 实例化 ——
 * 那一刻候选还没登记，直接注入会抛 {@code NoSuchBeanDefinitionException}
 * （本轮已实测复现：去掉顺序保证后 3 条用例全红）。
 * {@code @DependsOn} 把「先建容器、再建控制器」这个顺序<b>显式化</b>。
 *
 * <p><b>注意这是消费方契约，starter 不会替你做</b>。可选写法三种，任选其一：
 * <ol>
 *   <li>{@code @DependsOn("jvfaultModuleContainer")}（本例所用，最直接）；</li>
 *   <li>注入点加 {@code @Lazy} —— 把解析推迟到首次使用；</li>
 *   <li>用 {@code ObjectProvider<Greeter>} 按需取。</li>
 * </ol>
 *
 * <p><b>为什么 starter 不能自动解决</b>（架构性限制，本轮已实测 + 反汇编确认）：
 * <ul>
 *   <li>要让顺序自动正确，就得把「登记 jvfault bean」提前到
 *       {@code BeanFactoryPostProcessor} 阶段 —— 但反向注入（Spring → jvfault）
 *       要求 Spring bean <b>已就绪</b>，而 BFPP 阶段用户 bean 一个都还没创建，
 *       两者在时间上<b>直接冲突</b>；</li>
 *   <li>{@code preInstantiateSingletons()} 遍历的是 {@code beanDefinitionNames}
 *       <b>快照</b>，自动配置 {@code @Bean} 里新登记的定义不进本轮预实例化。</li>
 * </ul>
 *
 * @since v1.0.14 (2026)
 */
@RestController
@DependsOn("jvfaultModuleContainer")
public class DemoController {

    private final Greeter greeter;

    public DemoController(Greeter greeter) {
        this.greeter = greeter;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "world") String name) {
        return greeter.greet(name);
    }
}
