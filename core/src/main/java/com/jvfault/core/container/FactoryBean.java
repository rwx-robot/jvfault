package com.jvfault.core.container;

/**
 * 工厂 Bean 接口 - 延迟创建复杂对象。
 * 对应 Spring: FactoryBean
 *
 * <p>通过 {@link BeanRegistry#registerFactory(String, Class, FactoryBean)}
 * 注册后，容器在首次解析该 Bean 时调用 {@link #getObject()}。
 *
 * @param <T> 产出的 Bean 类型
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@FunctionalInterface
public interface FactoryBean<T> {

    /**
     * 创建并返回 Bean 实例（容器仅调用一次，结果按定义的作用域缓存）。
     */
    T getObject() throws Exception;
}
