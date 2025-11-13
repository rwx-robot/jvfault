package com.jvfault.core.container;

import com.jvfault.core.annotation.Component;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 Bean 注册表实现
 *
 * <p>特性：
 * <ul>
 *   <li>三级缓存解决字段/Setter 注入的循环依赖 (singletonFactories -> earlySingletonObjects -> singletonObjects)</li>
 *   <li>支持构造器/字段/Setter 注入</li>
 *   <li>作用域管理: Singleton/Prototype/Request</li>
 *   <li>线程安全</li>
 * </ul>
 *
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public class DefaultBeanRegistry implements BeanRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultBeanRegistry.class);

    // ============ 核心存储 ============

    /** Bean 定义注册表 (name -> BeanDefinition) */
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();

    /** 注册顺序 (providers 声明顺序，保证确定性初始化) */
    private final List<String> registrationOrder = Collections.synchronizedList(new ArrayList<>());

    /** 单例缓存 (name -> instance) */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    /** 早期引用缓存 (name -> instance) - 暴露已实例化未填充的 Bean，解决循环依赖 */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();

    /** 正在创建的 Bean (用于循环依赖检测) */
    private final Set<String> creatingBeans = ConcurrentHashMap.newKeySet();

    /** 类型索引 (type -> Set<name>) */
    private final Map<Class<?>, Set<String>> typeIndex = new ConcurrentHashMap<>();

    /** 别名映射 (alias -> canonicalName) */
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    /** 父容器 (层级容器支持) */
    private volatile BeanRegistry parent;

    /** 已就绪的 Bean 后处理器 (初始化序列回调) */
    private volatile List<BeanPostProcessor> beanPostProcessors = Collections.emptyList();

    // ============ 注册 API ============

    @Override
    public BeanRegistry registerBean(Class<?> beanClass, BeanDefinition definition) {
        String name = definition.getName();

        beanDefinitions.put(name, definition);
        if (!registrationOrder.contains(name)) {
            registrationOrder.add(name);
        }

        // 更新类型索引：实现类、接口、父类
        Class<?> clazz = beanClass;
        while (clazz != null && clazz != Object.class) {
            typeIndex.computeIfAbsent(clazz, k -> ConcurrentHashMap.newKeySet()).add(name);
            for (Class<?> iface : clazz.getInterfaces()) {
                typeIndex.computeIfAbsent(iface, k -> ConcurrentHashMap.newKeySet()).add(name);
            }
            clazz = clazz.getSuperclass();
        }

        // 处理限定符别名（不包含自身名称，避免别名解析死循环）
        for (String qualifier : definition.getQualifiers()) {
            if (!qualifier.isEmpty() && !qualifier.equals(name)) {
                aliases.put(qualifier, name);
            }
        }

        log.debug("Registered bean: {} [{}]", name, beanClass.getName());
        return this;
    }

    @Override
    public BeanRegistry registerSingleton(Class<?> beanClass, Object instance) {
        return registerSingleton(defaultBeanName(beanClass), instance);
    }

    @Override
    public BeanRegistry registerSingleton(String name, Object instance) {
        singletonObjects.put(name, instance);
        BeanDefinition def = new BeanDefinition(name, instance.getClass());
        def.setScope(Component.Scope.SINGLETON);
        def.setInstance(instance);
        def.setInitialized(true);
        beanDefinitions.put(name, def);
        if (!registrationOrder.contains(name)) {
            registrationOrder.add(name);
        }
        indexType(name, instance.getClass());
        log.debug("Registered singleton instance: {}", name);
        return this;
    }

    @Override
    public <T> BeanRegistry registerFactory(String name, Class<T> type, FactoryBean<T> factory) {
        BeanDefinition def = new BeanDefinition(name, type);
        def.setFactoryBean(factory);
        registerBean(type, def);
        return this;
    }

    private void indexType(String name, Class<?> beanClass) {
        Class<?> clazz = beanClass;
        while (clazz != null && clazz != Object.class) {
            typeIndex.computeIfAbsent(clazz, k -> ConcurrentHashMap.newKeySet()).add(name);
            for (Class<?> iface : clazz.getInterfaces()) {
                typeIndex.computeIfAbsent(iface, k -> ConcurrentHashMap.newKeySet()).add(name);
            }
            clazz = clazz.getSuperclass();
        }
    }

    // ============ 解析 API ============

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        String[] names = getBeanNamesForType(requiredType);
        if (names.length == 0) {
            throw new NoSuchBeanDefinitionException("No bean found for type: " + requiredType.getName());
        }
        if (names.length > 1) {
            for (String name : names) {
                BeanDefinition def = beanDefinitions.get(name);
                if (def != null && def.isPrimary()) {
                    return (T) getBean(name, requiredType);
                }
            }
            throw new NoUniqueBeanDefinitionException("Multiple beans found for type: " + requiredType.getName()
                    + ", candidates: " + Arrays.toString(names));
        }
        return (T) getBean(names[0], requiredType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = getBean(name);
        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw new IllegalStateException("Bean '" + name + "' is not of type " + requiredType.getName());
        }
        return (T) bean;
    }

    @Override
    public Object getBean(String name) {
        String canonicalName = resolveAlias(name);
        BeanDefinition def = beanDefinitions.get(canonicalName);
        if (def == null) {
            if (parent != null) {
                return parent.getBean(name);
            }
            throw new NoSuchBeanDefinitionException("No bean named '" + name + "' found");
        }
        return getBeanInstance(canonicalName, def);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> requiredType) {
        return () -> getBean(requiredType);
    }

    @Override
    public <T> Provider<T> getProvider(String name, Class<T> requiredType) {
        return () -> getBean(name, requiredType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Map<String, T> result = new LinkedHashMap<>();
        Set<String> names = typeIndex.get(type);
        if (names != null) {
            for (String name : names) {
                result.put(name, (T) getBean(name));
            }
        }
        if (parent != null) {
            result.putAll(parent.getBeansOfType(type));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public <A extends Annotation> Map<String, Object> getBeansWithAnnotation(Class<A> annotationType) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : beanDefinitions.keySet()) {
            BeanDefinition def = beanDefinitions.get(name);
            if (def != null && def.getBeanClass().isAnnotationPresent(annotationType)) {
                result.put(name, getBean(name));
            }
        }
        if (parent != null) {
            result.putAll(parent.getBeansWithAnnotation(annotationType));
        }
        return Collections.unmodifiableMap(result);
    }

    // ============ 生命周期 API ============

    @Override
    public void initializeSingletons() {
        // 按 providers 声明顺序：先创建所有 BeanPostProcessor，再初始化其余单例
        for (String name : new ArrayList<>(registrationOrder)) {
            BeanDefinition def = beanDefinitions.get(name);
            if (def != null && BeanPostProcessor.class.isAssignableFrom(def.getBeanClass())
                    && !singletonObjects.containsKey(name)) {
                getBean(name);
            }
        }
        refreshPostProcessors();

        for (String name : new ArrayList<>(registrationOrder)) {
            BeanDefinition def = beanDefinitions.get(name);
            if (def != null && def.getScope() == Component.Scope.SINGLETON
                    && !def.isLazyInit() && !singletonObjects.containsKey(name)) {
                getBean(name); // 触发初始化（依赖级联创建）
            }
        }
    }

    /**
     * 收集已实例化的 BeanPostProcessor（仅使用已就绪实例，避免创建期递归）。
     */
    private void refreshPostProcessors() {
        List<BeanPostProcessor> list = new ArrayList<>();
        for (BeanDefinition def : beanDefinitions.values()) {
            if (BeanPostProcessor.class.isAssignableFrom(def.getBeanClass())
                    && def.getInstance() instanceof BeanPostProcessor) {
                list.add((BeanPostProcessor) def.getInstance());
            }
        }
        this.beanPostProcessors = Collections.unmodifiableList(list);
    }

    @Override
    public void destroySingletons() {
        List<String> names = new ArrayList<>(singletonObjects.keySet());
        Collections.reverse(names);
        for (String name : names) {
            destroyBean(name);
        }
        singletonObjects.clear();
        earlySingletonObjects.clear();
        creatingBeans.clear();
    }

    @Override
    public void registerShutdownHook(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }

    // ============ 内省 API ============

    @Override
    public boolean containsBean(Class<?> type) {
        return typeIndex.containsKey(type) || (parent != null && parent.containsBean(type));
    }

    @Override
    public boolean containsBean(String name) {
        return beanDefinitions.containsKey(resolveAlias(name))
                || (parent != null && parent.containsBean(name));
    }

    @Override
    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitions.get(resolveAlias(name));
    }

    @Override
    public Set<String> getBeanNames() {
        return Collections.unmodifiableSet(beanDefinitions.keySet());
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        Set<String> names = typeIndex.get(type);
        if (names == null || names.isEmpty()) {
            if (parent != null) {
                return parent.getBeanNamesForType(type);
            }
            return new String[0];
        }
        return names.toArray(new String[0]);
    }

    @Override
    public boolean isSingleton(String name) {
        BeanDefinition def = beanDefinitions.get(resolveAlias(name));
        return def != null && def.getScope() == Component.Scope.SINGLETON;
    }

    @Override
    public boolean isPrototype(String name) {
        BeanDefinition def = beanDefinitions.get(resolveAlias(name));
        return def != null && def.getScope() == Component.Scope.PROTOTYPE;
    }

    @Override
    public Class<?> getType(String name) {
        BeanDefinition def = beanDefinitions.get(resolveAlias(name));
        return def != null ? def.getBeanClass() : null;
    }

    @Override
    public String[] getAliases(String name) {
        String canonical = resolveAlias(name);
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (canonical.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result.toArray(new String[0]);
    }

    // ============ 父子容器 ============

    @Override
    public void setParent(BeanRegistry parent) {
        this.parent = parent;
    }

    @Override
    public BeanRegistry getParent() {
        return parent;
    }

    // ============ 作用域支持 ============

    @Override
    public Object enterRequestScope() {
        return new Object();
    }

    @Override
    public void exitRequestScope(Object scopeId) {
        // 请求作用域 Bean 清理由 Web 容器负责
    }

    // ============ 内部实现 ============

    private Object getBeanInstance(String name, BeanDefinition def) {
        Component.Scope scope = def.getScope();
        if (scope == Component.Scope.SINGLETON || scope == Component.Scope.REQUEST) {
            return getSingleton(name, def);
        } else if (scope == Component.Scope.PROTOTYPE) {
            return createBean(name, def);
        }
        throw new IllegalStateException("Unknown scope: " + scope);
    }

    /**
     * 获取单例 Bean（含循环依赖处理）
     */
    private Object getSingleton(String name, BeanDefinition def) {
        // 1. 成品缓存
        Object singleton = singletonObjects.get(name);
        if (singleton != null) {
            return singleton;
        }

        // 2. 早期引用（已实例化、未填充依赖）—— 循环依赖场景
        singleton = earlySingletonObjects.get(name);
        if (singleton != null) {
            return singleton;
        }

        // 3. 防御性检测（理论上不会到达）
        if (creatingBeans.contains(name)) {
            throw new IllegalStateException("Circular dependency detected: " + name);
        }

        // 4. 创建
        creatingBeans.add(name);
        try {
            singleton = createBean(name, def);
            singletonObjects.put(name, singleton);
            return singleton;
        } catch (Exception e) {
            throw (e instanceof RuntimeException) ? (RuntimeException) e
                    : new IllegalStateException("Failed to create bean: " + name, e);
        } finally {
            creatingBeans.remove(name);
            earlySingletonObjects.remove(name);
        }
    }

    /**
     * 创建 Bean 实例：实例化 -> 暴露早期引用 -> 依赖填充 -> 初始化序列
     */
    private Object createBean(String name, BeanDefinition def) {
        Object instance;

        if (def.getFactoryBean() != null) {
            instance = createFromFactoryBean(def);
        } else if (def.getFactoryMethod() != null) {
            instance = createFromFactoryMethod(name, def);
        } else {
            instance = createFromConstructor(name, def);
        }

        // 在填充依赖前暴露早期引用，供循环依赖方获取
        earlySingletonObjects.put(name, instance);

        injectFields(instance);
        injectMethods(instance);

        // 初始化序列: before -> @PostConstruct/initMethod -> after
        instance = applyPostProcessorsBeforeInit(name, instance);
        invokeInitMethods(def, instance);
        instance = applyPostProcessorsAfterInit(name, instance);

        def.setInstance(instance);
        if (instance instanceof BeanPostProcessor) {
            refreshPostProcessors();
        }
        return instance;
    }

    private Object applyPostProcessorsBeforeInit(String name, Object bean) {
        Object current = bean;
        for (BeanPostProcessor bpp : beanPostProcessors) {
            try {
                Object result = bpp.postProcessBeforeInitialization(current, name);
                if (result != null) {
                    current = result;
                }
            } catch (Exception e) {
                throw new IllegalStateException("BeanPostProcessor.before failed for bean: " + name, e);
            }
        }
        return current;
    }

    private Object applyPostProcessorsAfterInit(String name, Object bean) {
        Object current = bean;
        for (BeanPostProcessor bpp : beanPostProcessors) {
            try {
                Object result = bpp.postProcessAfterInitialization(current, name);
                if (result != null) {
                    current = result;
                }
            } catch (Exception e) {
                throw new IllegalStateException("BeanPostProcessor.after failed for bean: " + name, e);
            }
        }
        return current;
    }

    /**
     * 调用初始化回调: @PostConstruct (含父类) 与 @Bean initMethod
     */
    private void invokeInitMethods(BeanDefinition def, Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(jakarta.annotation.PostConstruct.class)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to invoke @PostConstruct on " + def.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        String initMethod = def.getInitMethodName();
        if (initMethod != null && !initMethod.isEmpty() && !"(inferred)".equals(initMethod)) {
            try {
                Method method = instance.getClass().getMethod(initMethod);
                method.setAccessible(true);
                method.invoke(instance);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new IllegalStateException("Failed to invoke initMethod '" + initMethod
                        + "' on " + def.getName(), e);
            }
        }
        def.setInitialized(true);
    }

    private Object createFromFactoryBean(BeanDefinition def) {
        try {
            return def.getFactoryBean().getObject();
        } catch (Exception e) {
            throw new IllegalStateException("FactoryBean failed to create: " + def.getName(), e);
        }
    }

    private Object createFromConstructor(String name, BeanDefinition def) {
        Constructor<?> ctor = def.getConstructor();
        if (ctor == null) {
            throw new IllegalStateException(
                    "No suitable constructor for: " + def.getBeanClass().getName()
                            + " (需要公共构造器或 @Inject 构造器)");
        }

        Parameter[] params = ctor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Named named = param.getAnnotation(Named.class);
            String qualifier = named != null ? named.value() : "";
            args[i] = resolveDependency(param.getType(), qualifier, true,
                    def.getName() + " constructor parameter " + i);
        }

        try {
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate: " + def.getBeanClass().getName(), e);
        }
    }

    private Object createFromFactoryMethod(String name, BeanDefinition def) {
        Method method = def.getFactoryMethod();
        Class<?> configClass = def.getFactoryClass();

        // 配置类实例本身也由容器管理
        Object configInstance = getBean(configClass);

        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Named named = param.getAnnotation(Named.class);
            String qualifier = named != null ? named.value() : "";
            args[i] = resolveDependency(param.getType(), qualifier, true,
                    "@Bean " + method.getName() + " parameter " + i);
        }

        try {
            method.setAccessible(true);
            return method.invoke(configInstance, args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke @Bean method: " + method.getName(), e);
        }
    }

    /**
     * 字段注入：扫描本类及父类中标注 @Inject 的字段
     */
    private void injectFields(Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!isInjectable(field)) {
                    continue;
                }
                Named named = field.getAnnotation(Named.class);
                String qualifier = named != null ? named.value() : "";
                Object value = resolveDependency(field.getType(), qualifier, true,
                        instance.getClass().getName() + "." + field.getName());
                try {
                    field.setAccessible(true);
                    field.set(instance, value);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to inject field: "
                            + clazz.getName() + "." + field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Setter/方法注入：扫描标注 @Inject 的方法
     */
    private void injectMethods(Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isInjectable(method)) {
                    continue;
                }
                Parameter[] params = method.getParameters();
                Object[] args = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    Named named = params[i].getAnnotation(Named.class);
                    String qualifier = named != null ? named.value() : "";
                    args[i] = resolveDependency(params[i].getType(), qualifier, true,
                            instance.getClass().getName() + "." + method.getName());
                }
                try {
                    method.setAccessible(true);
                    method.invoke(instance, args);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke @Inject method: "
                            + clazz.getName() + "." + method.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean isInjectable(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && (field.isAnnotationPresent(jakarta.inject.Inject.class)
                        || field.isAnnotationPresent(com.jvfault.core.annotation.Inject.class));
    }

    private boolean isInjectable(Method method) {
        return !Modifier.isStatic(method.getModifiers())
                && (method.isAnnotationPresent(jakarta.inject.Inject.class)
                        || method.isAnnotationPresent(com.jvfault.core.annotation.Inject.class));
    }

    /**
     * 解析单个依赖：限定符 -> 类型 -> @Primary -> 父容器
     */
    private Object resolveDependency(Class<?> type, String qualifier, boolean required, String description) {
        // 1. 按限定符
        if (qualifier != null && !qualifier.isEmpty()) {
            String beanName = aliases.get(qualifier);
            if (beanName != null) {
                return getBean(beanName);
            }
        }

        // 2. 按类型
        String[] names = getBeanNamesForType(type);
        if (names.length == 1) {
            return getBean(names[0]);
        }
        if (names.length > 1) {
            for (String name : names) {
                BeanDefinition def = beanDefinitions.get(name);
                if (def != null && def.isPrimary()) {
                    return getBean(name);
                }
            }
            if (required) {
                throw new NoUniqueBeanDefinitionException("Multiple candidates for " + type.getName()
                        + " while resolving " + description + ", specify @Named or @Primary: "
                        + Arrays.toString(names));
            }
            return null;
        }

        // 3. 父容器
        if (parent != null && parent.containsBean(type)) {
            return parent.getBean(type);
        }

        if (required) {
            throw new NoSuchBeanDefinitionException(
                    "No bean found for type: " + type.getName() + " while resolving " + description);
        }
        return null;
    }

    private void destroyBean(String name) {
        // @PreDestroy 回调由 ModuleContainer 按模块反序调用
        singletonObjects.remove(name);
    }

    private String resolveAlias(String name) {
        String resolved = name;
        while (aliases.containsKey(resolved)) {
            String next = aliases.get(resolved);
            if (next.equals(resolved)) {
                break;
            }
            resolved = next;
        }
        return resolved;
    }

    private String defaultBeanName(Class<?> clazz) {
        String simple = clazz.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }
}
