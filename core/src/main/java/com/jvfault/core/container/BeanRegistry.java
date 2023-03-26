package com.jvfault.core.container;

import com.jvfault.core.annotation.Component;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

/**
 * Bean 注册表 - 核心 IoC 容器接口
 * 
 * <p>职责：
 * <ul>
 *   <li>Bean 定义注册与解析</li>
 *   <li>实例创建与生命周期管理</li>
 *   <li>依赖解析与注入</li>
 *   <li>作用域管理 (Singleton/Prototype/Request)</li>
 * </ul>
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public interface BeanRegistry {

    // ============ 注册 API ============

    /**
     * 注册 Bean 定义 (来自 @Component 扫描或 @Bean 方法)
     * 
     * @param beanClass Bean 实现类
     * @param definition Bean 定义元数据
     * @return this (链式调用)
     */
    BeanRegistry registerBean(Class<?> beanClass, BeanDefinition definition);

    /**
     * 注册单例实例 (已创建的对象)
     * 
     * @param beanClass Bean 类型
     * @param instance 实例对象
     * @return this
     */
    BeanRegistry registerSingleton(Class<?> beanClass, Object instance);

    /**
     * 注册单例实例 (指定名称)
     */
    BeanRegistry registerSingleton(String name, Object instance);

    /**
     * 注册 FactoryBean (延迟创建)
     */
    <T> BeanRegistry registerFactory(String name, Class<T> type, FactoryBean<T> factory);

    // ============ 解析 API ============

    /**
     * 按类型获取 Bean (单例)
     * 
     * @throws NoSuchBeanDefinitionException 未找到匹配 Bean
     * @throws NoUniqueBeanDefinitionException 找到多个匹配 Bean
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 按名称和类型获取 Bean
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 按名称获取 Bean (类型不检查)
     */
    Object getBean(String name);

    /**
     * 获取 Provider (延迟解析，支持原型作用域)
     */
    <T> Provider<T> getProvider(Class<T> requiredType);

    /**
     * 获取指定名称的 Provider
     */
    <T> Provider<T> getProvider(String name, Class<T> requiredType);

    /**
     * 获取所有指定类型的 Bean (Map<name, instance>)
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);

    /**
     * 获取所有带有指定注解的 Bean
     */
    <A extends Annotation> Map<String, Object> getBeansWithAnnotation(Class<A> annotationType);

    // ============ 生命周期 API ============

    /**
     * 初始化所有非懒加载的单例 Bean
     * 通常在容器启动时调用
     */
    void initializeSingletons();

    /**
     * 销毁所有单例 Bean (按依赖反序)
     * 通常在容器关闭时调用
     */
    void destroySingletons();

    /**
     * 注册关闭回调
     */
    void registerShutdownHook(Runnable hook);

    // ============ 内省 API ============

    /**
     * 检查是否包含指定类型的 Bean
     */
    boolean containsBean(Class<?> type);

    /**
     * 检查是否包含指定名称的 Bean
     */
    boolean containsBean(String name);

    /**
     * 获取 Bean 定义元数据
     */
    BeanDefinition getBeanDefinition(String name);

    /**
     * 获取所有 Bean 名称
     */
    Set<String> getBeanNames();

    /**
     * 获取指定类型的所有 Bean 名称
     */
    String[] getBeanNamesForType(Class<?> type);

    /**
     * 判断是否为单例
     */
    boolean isSingleton(String name);

    /**
     * 判断是否为原型
     */
    boolean isPrototype(String name);

    /**
     * 获取 Bean 类型
     */
    Class<?> getType(String name);

    /**
     * 获取 Bean 别名
     */
    String[] getAliases(String name);

    // ============ 父子容器支持 ============

    /**
     * 设置父容器 (用于层级容器)
     */
    void setParent(BeanRegistry parent);

    /**
     * 获取父容器
     */
    BeanRegistry getParent();

    // ============ 作用域支持 ============

    /**
     * 进入请求作用域 (Web 环境)
     * 返回作用域标识，用于退出时清理
     */
    Object enterRequestScope();

    /**
     * 退出请求作用域
     */
    void exitRequestScope(Object scopeId);
}