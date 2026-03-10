package com.jvfault.spring.boot.starter;

import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Spring Boot 3 自动配置：把 jvfault 容器接入 Spring 生命周期。
 *
 * <p><b>边界（插件化原则）</b>：
 * <ul>
 *   <li>依赖方向单向 —— 本 starter 依赖 core，core <b>不</b>依赖本模块、也不依赖 Spring。</li>
 *   <li>Spring 依赖是 {@code compileOnly}，不会传递进使用者的依赖树。</li>
 *   <li>激活必须显式声明 {@code jvfault.base-packages}（opt-in），放进来也不会有副作用。</li>
 * </ul>
 *
 * <p>容器生命周期交给 Spring：{@code destroyMethod = "destroy"} 保证应用关闭时销毁 bean。
 * 这里用 {@link JvfaultApplication#createContainer} 而不是 {@code run()} ——
 * 后者会阻塞并注册自己的 JVM shutdown hook，不适合交给 Spring 托管。
 *
 * @since v1.0.11 (2026)
 */
@AutoConfiguration
@ConditionalOnClass({ModuleContainer.class, JvfaultApplication.class})
@ConditionalOnProperty(prefix = "jvfault", name = "base-packages")
@EnableConfigurationProperties(JvfaultProperties.class)
public class JvfaultAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JvfaultAutoConfiguration.class);

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public ModuleContainer jvfaultModuleContainer(JvfaultProperties props, ConfigurableBeanFactory beanFactory) {
        Class<?> rootModule = resolveRootModule(props);
        ModuleContainer container = JvfaultApplication.createContainer(rootModule, props.getBasePackages());
        if (props.isExposeBeans()) {
            exposeToSpring(container.getBeanRegistry(), beanFactory);
        }
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanRegistry jvfaultBeanRegistry(ModuleContainer jvfaultModuleContainer) {
        return jvfaultModuleContainer.getBeanRegistry();
    }

    /**
     * jvfault → Spring 单向暴露：把 jvfault 容器里的 bean 注册成 Spring 单例，
     * 于是 Spring 组件可以直接 {@code @Autowired} 它们。
     *
     * <p>刻意<b>不做</b>反向（把 Spring bean 塞进 jvfault）—— 那会把两个容器的
     * 生命周期与依赖解析纠缠在一起，循环依赖很难诊断。需要时由使用者显式桥接。
     */
    private static void exposeToSpring(BeanRegistry registry, ConfigurableBeanFactory beanFactory) {
        int exposed = 0;
        for (String name : registry.getBeanNames()) {
            Object instance;
            try {
                instance = registry.getBean(name);
            } catch (RuntimeException ex) {
                log.debug("跳过无法实例化的 jvfault bean '{}'：{}", name, ex.getMessage());
                continue;
            }
            if (instance == null || beanFactory.containsSingleton(name)) {
                continue;
            }
            beanFactory.registerSingleton(name, instance);
            exposed++;
        }
        log.info("jvfault -> Spring：已暴露 {} 个 bean", exposed);
    }

    /** 确定根模块类：优先用显式配置，否则在 basePackages 下找唯一的 {@code @Module}。 */
    private static Class<?> resolveRootModule(JvfaultProperties props) {
        String explicit = props.getRootModule();
        if (explicit != null && !explicit.trim().isEmpty()) {
            try {
                return ClassUtils.forName(explicit.trim(), null);
            } catch (ClassNotFoundException | LinkageError e) {
                throw new IllegalStateException("jvfault.root-module 指定的类无法加载：" + explicit, e);
            }
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Module.class));

        Set<BeanDefinition> found = new LinkedHashSet<>();
        for (String pkg : props.getBasePackages()) {
            found.addAll(scanner.findCandidateComponents(pkg.trim()));
        }

        if (found.isEmpty()) {
            throw new IllegalStateException("在 jvfault.base-packages="
                    + String.join(",", props.getBasePackages())
                    + " 下没有找到 @Module 类；请添加一个，或用 jvfault.root-module 显式指定。");
        }
        if (found.size() > 1) {
            StringBuilder names = new StringBuilder();
            for (BeanDefinition def : found) {
                names.append('\n').append("  - ").append(def.getBeanClassName());
            }
            throw new IllegalStateException("在 jvfault.base-packages 下发现 " + found.size()
                    + " 个 @Module 类，无法确定根模块；请用 jvfault.root-module 显式指定。候选："
                    + names);
        }

        String className = found.iterator().next().getBeanClassName();
        try {
            return ClassUtils.forName(className, null);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException("无法加载根模块类 " + className, e);
        }
    }
}
