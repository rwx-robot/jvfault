package com.jvfault.spring.boot.starter.reversefixture;

import com.jvfault.spring.boot.starter.JvfaultComponent;

/**
 * 「手工注册单例」场景的 fixture：不参与组件扫描，由测试直接
 * {@code beanFactory.registerSingleton(...)} 塞进 Spring —— 它<b>没有</b> BeanDefinition。
 *
 * <p>用它覆盖 {@code getBeanNamesForAnnotation} 命中手工单例的那条分支。
 * 注意：{@code withBean(...)} 永远会生成 BeanDefinition，<b>结构上测不到</b>这条路径，
 * 所以必须用 {@code registerSingleton} 才能复现（这正是该缺陷能一路潜行的原因）。
 */
@JvfaultComponent
public class ManualClock {

    public String now() {
        return "manual";
    }
}
