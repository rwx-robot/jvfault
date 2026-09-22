package com.jvfault.core.container;

/**
 * Bean 后处理器 - 容器的核心扩展点。
 * 对应 Spring: BeanPostProcessor
 *
 * <p>实现类本身也是容器 Bean（通过模块 providers 注册或
 * {@link BeanRegistry#registerSingleton(String, Object)} 注册）。
 * 容器会在每个 Bean 的初始化序列中回调所有已注册的后处理器：
 *
 * <pre>实例化 → 依赖注入 → before → @PostConstruct/initMethod → after</pre>
 *
 * <p>典型用途：AOP 代理包装、@Value 占位符注入、@Scheduled 任务注册等。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public interface BeanPostProcessor {

    /**
     * 初始化回调（@PostConstruct / initMethod）之前调用。
     *
     * @param bean 已完成依赖注入的实例
     * @param beanName Bean 名称
     * @return 处理后的实例（默认原样返回）
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 初始化回调之后调用（可返回代理对象替换原实例）。
     *
     * @param bean 已完成初始化的实例
     * @param beanName Bean 名称
     * @return 处理后的实例（默认原样返回）
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
