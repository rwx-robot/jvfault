package com.jvfault.aop.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 切面注册表 - 收集容器中的 @Aspect Bean。
 *
 * <p>顺序约束：切面 Bean 需先于目标 Bean 被容器创建（推荐放入
 * 全局模块或根模块 providers 的最前面），否则对已创建的 Bean 不生效。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class AspectRegistry {

    private static final Logger log = Logger.getLogger(AspectRegistry.class.getName());

    private final List<Advisor> advisors = new CopyOnWriteArrayList<>();

    /**
     * 注册切面 Bean（非 @Aspect 类忽略）。
     */
    public void registerAspect(Object candidate) {
        if (candidate != null
                && candidate.getClass().isAnnotationPresent(com.jvfault.aop.annotation.Aspect.class)) {
            advisors.add(new Advisor(candidate));
            log.fine("Registered aspect: " + candidate.getClass().getName());
        }
    }

    /**
     * 按 @Order 升序（数值小优先）返回全部 Advisor。
     */
    public List<Advisor> getAdvisors() {
        List<Advisor> sorted = new ArrayList<>(advisors);
        sorted.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        return sorted;
    }

    public boolean isEmpty() {
        return advisors.isEmpty();
    }
}
