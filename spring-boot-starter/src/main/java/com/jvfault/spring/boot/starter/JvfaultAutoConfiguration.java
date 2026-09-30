package com.jvfault.spring.boot.starter;

import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.container.DefaultBeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

        // 直接组合 core 的公开低层 API（DefaultBeanRegistry + ModuleContainer + refresh），
        // 而不是走 JvfaultApplication.createContainer —— 「先登记外部 bean、再 refresh」
        // 是本模块自己的编排需求，不该为此给 core 加 API（core 保持零 Spring 语义）。
        BeanRegistry beanRegistry = new DefaultBeanRegistry();
        ModuleContainer container = new ModuleContainer(beanRegistry);

        // 反向注入（Spring → jvfault）必须在 refresh 之前完成：jvfault 的 @Component
        // 在 initializeSingletons() 阶段就解析 @Inject，那时 Spring bean 必须已在注册表里。
        Set<String> importedNames = props.isImportSpringBeans()
                ? importSpringBeans(beanRegistry, beanFactory)
                : Collections.emptySet();

        try {
            container.refresh(rootModule, props.getBasePackages());
        } catch (RuntimeException ex) {
            throw bridgeFailure("jvfault 容器刷新（扫描 + 实例化单例）失败", ex);
        }

        if (props.isExposeBeans()) {
            exposeToSpring(container.getBeanRegistry(), beanFactory, importedNames);
        }
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanRegistry jvfaultBeanRegistry(ModuleContainer jvfaultModuleContainer) {
        return jvfaultModuleContainer.getBeanRegistry();
    }

    /**
     * jvfault → Spring 单向暴露：为 jvfault 容器里的每个 bean 注册一个 Spring
     * {@code BeanDefinition}，于是 Spring 组件可以按类型/名字注入它们。
     *
     * <p><b>为什么是「BeanDefinition + FactoryBean 桥接」</b>（三种写法对比，结论来自实测）：
     * <ul>
     *   <li>{@code registerSingleton}：类型匹配对用户 bean 不可见（刷新中期才登记），
     *       消费方被迫加 {@code @Lazy}；但所有权清晰、无重复生命周期。</li>
     *   <li>{@code BeanDefinition + instanceSupplier}：<b>不可用</b> —— Spring 会把它当成
     *       自己创建的对象，走完整 {@code doCreateBean}（{@code populateBean} → 前置回调 →
     *       {@code @PostConstruct}/init → 登记销毁回调），于是 jvfault 已经做过的生命周期
     *       <b>被再做一遍</b>：{@code @PostConstruct} 两次、{@code @PreDestroy} 两次，
     *       {@code @Autowired} 还会误注入到 jvfault 的对象上，AOP 再包一层代理。</li>
     *   <li><b>本实现</b>：{@code BeanDefinition} 指向一个委托 {@link FactoryBean}。
     *       Spring 对 <b>FactoryBean 生产出来的对象</b>不登记销毁回调、也不走
     *       {@code populateBean}/init 回调 —— 所有权仍归 jvfault，桥接只负责"取"。</li>
     * </ul>
     *
     * <p>{@code setTargetType} 是类型可见性的关键：工厂本身还没实例化时，Spring 的
     * {@code isTypeMatch} 走 {@code predictBeanType} 读到它，候选解析因此能命中。
     * （注意：这里必须用 {@code setTargetType} 而不是 {@code setBeanClass(实际类型)} ——
     *  后者会让 Spring 尝试实例化那个类，而产品其实是 {@code getObject()} 给的，
     *  类型是子类时会 ClassCastException。）
     *
     * @param importedNames 本轮反向导入进 jvfault 的 Spring bean 名 —— 这些<b>绝不能</b>
     *                      再倒回 Spring，否则同一个实例会以两个身份出现（重复单例）。
     */
    private static void exposeToSpring(BeanRegistry registry, ConfigurableBeanFactory beanFactory,
            Set<String> importedNames) {
        if (!(beanFactory instanceof BeanDefinitionRegistry bdr)) {
            log.warn("Spring bean factory 不是 BeanDefinitionRegistry，跳过 jvfault → Spring 暴露");
            return;
        }
        int exposed = 0;
        for (String name : registry.getBeanNames()) {
            if (importedNames.contains(name)) {
                continue;
            }
            if (bdr.containsBeanDefinition(name)) {
                log.warn("Spring 中已存在同名 bean 定义 '{}'，跳过暴露（不覆盖使用者的定义）", name);
                continue;
            }
            Class<?> type = registry.getType(name);
            RootBeanDefinition def = new RootBeanDefinition();
            def.setTargetType(type != null ? type : Object.class);
            def.setInstanceSupplier(() -> registry.getBean(name));
            def.setScope(BeanDefinition.SCOPE_SINGLETON);
            try {
                bdr.registerBeanDefinition(name, def);
                exposed++;
            } catch (RuntimeException ex) {
                log.warn("暴露 jvfault bean '{}' 到 Spring 失败，跳过：{}", name, ex.getMessage());
            }
        }
        log.info("jvfault -> Spring：已暴露 {} 个 bean", exposed);
    }

    /**
     * jvfault → Spring 的委托工厂：产品直接取自 jvfault 容器。
     *
     * <p>刻意<b>不</b>让 Spring 拥有这个对象的生命周期 —— 不登记销毁回调、
     * 不做属性填充、不跑初始化回调，这些 jvfault 容器已经做过了。
     */
    private static final class JvfaultBeanBridge<T> implements FactoryBean<T> {

        private final String beanName;
        private final Class<?> type;
        private final BeanRegistry registry;

        private JvfaultBeanBridge(String beanName, Class<?> type, BeanRegistry registry) {
            this.beanName = beanName;
            this.type = type;
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getObject() {
            return (T) registry.getBean(beanName);
        }

        @Override
        public Class<?> getObjectType() {
            return type;
        }

        /**
         * 产品作用域取自 jvfault：prototype 组件在 Spring 侧也应每次取到新实例，
         * 不能被压平成单例。
         */
        @Override
        public boolean isSingleton() {
            return !registry.isPrototype(beanName);
        }
    }

    /**
     * Spring → jvfault 反向注入：把标注了 {@link JvfaultComponent} 的 Spring 单例
     * 登记进 jvfault 的 {@link BeanRegistry}（作为已就绪单例）。
     *
     * <p>设计取舍（避免把两个容器纠缠在一起）：
     * <ul>
     *   <li><b>只导入显式标注的 bean</b> —— 不倒灌整个 Spring 容器；</li>
     *   <li><b>只导入 singleton 作用域</b> —— request / prototype 等在 jvfault 里无法托管；</li>
     *   <li><b>沿用 Spring 的 bean 名</b> —— 与 Spring 侧同名，既天然避免重复注册，
     *       也让两个容器的 bean 名一一对应，便于排查。</li>
     * </ul>
     *
     * <p>必须在 refresh 之前调用：jvfault 的 {@code @Component} 在
     * {@code initializeSingletons()} 阶段解析 {@code @Inject}，那时这些 bean 必须已就位。
     *
     * <p><b>失败即抛</b>：实例化失败几乎总是「跨容器循环依赖」（该 Spring bean 又依赖了
     * jvfault bean）。静默吞掉只会换来后面更难懂的 NPE，所以这里给出明确提示并中断启动。
     *
     * @return 被导入的 bean 名集合，供 {@link #exposeToSpring} 排除回环
     */
    private static Set<String> importSpringBeans(BeanRegistry registry, ConfigurableBeanFactory beanFactory) {
        Set<String> imported = new LinkedHashSet<>();
        if (!(beanFactory instanceof ConfigurableListableBeanFactory lbf)) {
            log.warn("Spring bean factory 不是 ConfigurableListableBeanFactory，跳过反向注入");
            return imported;
        }
        // 注意：getBeanNamesForAnnotation 同时扫描 beanDefinitionNames 与「手工注册单例」，
        // 后者<b>没有</b> BeanDefinition —— 对它调 getBeanDefinition 会抛
        // NoSuchBeanDefinitionException。必须先用 containsBeanDefinition 分流，
        // 否则手工单例会被静默跳过（反向注入悄悄失效，最难排查）。
        List<Throwable> failures = new ArrayList<>();
        for (String beanName : lbf.getBeanNamesForAnnotation(JvfaultComponent.class)) {
            boolean hasDefinition = lbf.containsBeanDefinition(beanName);
            BeanDefinition def = null;
            if (hasDefinition) {
                try {
                    def = lbf.getBeanDefinition(beanName);
                } catch (RuntimeException ex) {
                    log.warn("读取被 @JvfaultComponent 标注的 bean '{}' 定义失败，跳过", beanName, ex);
                    continue;
                }
            }
            if (def != null) {
                if (!def.isSingleton()) {
                    log.warn("跳过非单例 Spring bean '{}'（作用域 '{}' 非 singleton，jvfault 无法托管）",
                            beanName, def.getScope());
                    continue;
                }
                // 反向注入必然要拿到实例，等于强制提前实例化 —— 那会破坏使用者的 @Lazy 语义，
                // 所以明确跳过并告警，而不是偷偷把它造出来。
                if (def.isLazyInit()) {
                    log.warn("跳过 lazy-init 的 Spring bean '{}'：反向注入会强制提前实例化，"
                            + "破坏 @Lazy 语义（若确需导入，请去掉该 bean 的 lazy-init）", beanName);
                    continue;
                }
            }
            // 目标类型必须在 getBean **之前**取：实例化之后 getType 可能返回 CGLIB 代理类，
            // 那样就失去了「按接口/父类注入」的能力。
            Class<?> targetType = lbf.getType(beanName);
            Object instance;
            try {
                // 统一用 getBean：对「有 definition」会正常创建/返回；对「手工注册单例」
                // 会直接返回已注册的实例。**不能用 getSingleton()** —— 它只查缓存，
                // 而自动配置执行时 Spring 往往还没实例化，会拿到 null 让反向注入静默退化。
                instance = beanFactory.getBean(beanName);
            } catch (RuntimeException ex) {
                throw bridgeFailure("实例化待导入的 Spring bean '" + beanName + "' 失败（常见原因："
                        + "① 跨容器循环依赖 —— 该 Spring bean 又依赖了 jvfault 的 bean；"
                        + "② 该 bean 在当前阶段尚未注册，例如手工注册单例晚于自动配置执行）", ex);
            }
            if (instance == null) {
                log.warn("待导入的 Spring bean '{}' 实例为 null（手工单例尚未就绪？），跳过", beanName);
                continue;
            }
            try {
                // 撞名自检：jvfault 侧已存在同名 bean 时，registerSingleton 会**静默覆盖**，
                // 事后表现为「注入到错误的实例」，极难排查。
                // 判据必须是「同名 **且** 不是同一个实例」才抛 —— 同一实例的重复登记是幂等的，
                // 必须放行，否则修掉重复暴露反而引入新的启动崩溃。
                if (registry.containsBean(beanName) && !imported.contains(beanName)) {
                    com.jvfault.core.container.BeanDefinition existing = registry.getBeanDefinition(beanName);
                    Object existingInstance = existing != null ? existing.getInstance() : null;
                    if (existingInstance != instance) {
                        throw new IllegalStateException("反向注入撞名：jvfault 容器中已有名为 '"
                                + beanName + "' 的 bean"
                                + (existingInstance != null
                                        ? "（实例类型 " + existingInstance.getClass().getName() + "）"
                                        : "（定义已存在、尚未实例化）")
                                + "，而待导入的 Spring bean 实例类型为 "
                                + instance.getClass().getName()
                                + "。请重命名 Spring 侧的 bean，或用 jvfault.root-module 隔离两个容器的命名空间");
                    }
                    log.debug("反向注入：bean '{}' 在 jvfault 侧已存在且为同一实例，幂等放行", beanName);
                }
                registry.registerSingleton(beanName, instance);
                // 代理场景补全类型索引：registerSingleton 只按 instance.getClass() 建索引，
                // 遇到 JDK 动态代理 / CGLIB 时，jvfault 侧按「接口或父类」@Inject 会解析不到。
                // 这里按 Spring 的目标类型再登记一次 —— 指向<b>同一个实例</b>，不是第二个 bean。
                if (targetType != null && targetType != instance.getClass()
                        && !registry.containsBean(targetType)) {
                    com.jvfault.core.container.BeanDefinition typed =
                            new com.jvfault.core.container.BeanDefinition(beanName, targetType);
                    typed.setScope(com.jvfault.core.annotation.Component.Scope.SINGLETON);
                    typed.setInstance(instance);
                    typed.setInitialized(true);
                    registry.registerBean(targetType, typed);
                }
                imported.add(beanName);
            } catch (RuntimeException ex) {
                // 只吞不传是反模式：把原始异常挂到 suppressed 上，最后统一 fail-fast，
                // 避免「启动看起来成功、运行时才炸」。
                log.error("反向注入 Spring bean '{}' 失败", beanName, ex);
                failures.add(ex);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException failure = bridgeFailure(
                    "有 " + failures.size() + " 个被 @JvfaultComponent 标注的 Spring bean 导入失败", null);
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
        log.info("Spring -> jvfault：已导入 {} 个 bean", imported.size());
        return imported;
    }

    /** 统一的桥接失败异常：把「跨容器」这个排查方向直接写进消息里。 */
    private static IllegalStateException bridgeFailure(String message, Throwable cause) {
        return new IllegalStateException("jvfault × Spring 桥接失败：" + message
                + "。排查方向：两容器间出现循环依赖时，请把其中一方改为 ObjectProvider/@Lazy 或用事件解耦。",
                cause);
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
