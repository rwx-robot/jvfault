package com.jvfault.cache;

import com.jvfault.cache.annotation.CacheEvict;
import com.jvfault.cache.annotation.CachePut;
import com.jvfault.cache.annotation.Cacheable;
import com.jvfault.cache.cache.CacheManager;
import com.jvfault.core.annotation.Component;
import com.jvfault.core.container.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存注解后处理器 - 为带缓存注解的 Bean 织入 JDK 代理。
 *
 * <p>约束：缓存 Bean 必须实现接口（JDK 动态代理）；无接口的类跳过并告警。
 * 注解方法与代理的语义：
 * <ul>
 *   <li>@Cacheable：命中返回缓存值（方法不执行），未命中执行并写入</li>
 *   <li>@CachePut：总是执行并更新缓存（键模板可用 #result）</li>
 *   <li>@CacheEvict：执行后驱逐（allEntries=true 清空缓存）</li>
 * </ul>
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
@Component
public class CacheableBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CacheableBeanPostProcessor.class);

    private final CacheManager cacheManager;
    private final KeyGenerator keyGenerator;

    /** beanClass -> 该类是否存在缓存注解（负缓存避免重复反射扫描） */
    private final ConcurrentHashMap<Class<?>, Boolean> annotatedCache = new ConcurrentHashMap<>();

    public CacheableBeanPostProcessor(CacheManager cacheManager) {
        this(cacheManager, KeyGenerator.DEFAULT);
    }

    public CacheableBeanPostProcessor(CacheManager cacheManager, KeyGenerator keyGenerator) {
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        if (clazz.isAnnotationPresent(com.jvfault.core.annotation.Component.class)
                && clazz.getInterfaces().length == 0 && !annotatedCache.containsKey(clazz)) {
            // 预扫描：无接口的类若有缓存注解，直接告警
        }
        Boolean hasAnnotations = annotatedCache.computeIfAbsent(clazz, this::scanForAnnotations);
        if (!hasAnnotations || clazz.getInterfaces().length == 0) {
            if (hasAnnotations && clazz.getInterfaces().length == 0) {
                log.warn("Bean '{}' 含缓存注解但未实现接口，无法代理（JDK 代理要求接口）", beanName);
            }
            return bean;
        }
        return java.lang.reflect.Proxy.newProxyInstance(
                clazz.getClassLoader(), clazz.getInterfaces(), new CacheHandler(bean));
    }

    private boolean scanForAnnotations(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(Cacheable.class)
                    || method.isAnnotationPresent(CachePut.class)
                    || method.isAnnotationPresent(CacheEvict.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 缓存拦截处理器。
     */
    class CacheHandler implements InvocationHandler {

        private final Object target;

        CacheHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, args);
            }
            // 接口方法 -> 实现类方法（注解通常标注在实现上）
            Method impl = findImpl(method);

            Cacheable cacheable = impl.getAnnotation(Cacheable.class);
            if (cacheable != null) {
                String[] cacheNames = cacheable.cacheNames();
                Object key = keyGenerator.generate(cacheable.key(), target, impl, args, null);
                for (String cacheName : cacheNames) {
                    Object cached = cacheManager.getCache(cacheName).get(key, Object.class);
                    if (cached != null) {
                        log.debug("Cache hit: {}#{}", cacheName, key);
                        return cached;
                    }
                }
                Object result = method.invoke(target, args);
                for (String cacheName : cacheNames) {
                    cacheManager.getCache(cacheName).put(key, result);
                }
                return result;
            }

            CachePut cachePut = impl.getAnnotation(CachePut.class);
            if (cachePut != null) {
                Object result = method.invoke(target, args);
                Object key = keyGenerator.generate(cachePut.key(), target, impl, args, result);
                for (String cacheName : cachePut.cacheNames()) {
                    cacheManager.getCache(cacheName).put(key, result);
                }
                return result;
            }

            CacheEvict evict = impl.getAnnotation(CacheEvict.class);
            if (evict != null) {
                Object result = method.invoke(target, args);
                for (String cacheName : evict.cacheNames()) {
                    if (evict.allEntries()) {
                        cacheManager.getCache(cacheName).clear();
                    } else {
                        Object key = keyGenerator.generate(evict.key(), target, impl, args, result);
                        cacheManager.getCache(cacheName).evict(key);
                    }
                }
                return result;
            }

            return method.invoke(target, args);
        }

        private Method findImpl(Method interfaceMethod) throws Throwable {
            try {
                return target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return interfaceMethod;
            }
        }
    }
}
