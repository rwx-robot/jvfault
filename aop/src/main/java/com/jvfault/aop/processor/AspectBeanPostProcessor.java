package com.jvfault.aop.processor;

import com.jvfault.aop.aspect.Advisor;
import com.jvfault.aop.aspect.AspectRegistry;
import com.jvfault.aop.proxy.ProxyFactory;
import com.jvfault.core.container.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 切面 Bean 后处理器 - 容器与 AOP 的桥接。
 *
 * <p>afterInitialization 阶段：
 * <ol>
 *   <li>若 Bean 是 @Aspect 类 → 注册进 {@link AspectRegistry}</li>
 *   <li>否则 → 若现有切面可匹配该 Bean 的方法，用 {@link ProxyFactory} 包装</li>
 * </ol>
 *
 * <p>注册方式（与 core 容器集成）：
 * <pre>{@code
 * ModuleContainer container = JvfaultApplication.run(AppModule.class);
 * // AppModule:
 * @Module(providers = {AspectRegistry.class, AspectBeanPostProcessor.class,
 *                      AuditingAspect.class, OrderService.class})
 * }</pre>
 *
 * <p>约束：切面 Bean 必须先于目标 Bean 创建（容器按 providers 顺序
 * 创建时把切面放前面，或放入 imports 列表更靠前的模块）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@com.jvfault.core.annotation.Component
public class AspectBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AspectBeanPostProcessor.class);

    private final AspectRegistry registry;

    public AspectBeanPostProcessor(AspectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        registry.registerAspect(bean);
        if (registry.isEmpty() || bean instanceof AspectRegistry
                || bean.getClass().isAnnotationPresent(com.jvfault.aop.annotation.Aspect.class)) {
            return bean;
        }

        List<Advisor> advisors = registry.getAdvisors();
        ProxyFactory factory = new ProxyFactory(advisors);
        Object proxied = factory.createProxy(bean);
        if (proxied != bean) {
            log.debug("Bean '{}' proxied by {} advisor(s)", beanName, advisors.size());
        }
        return proxied;
    }
}
