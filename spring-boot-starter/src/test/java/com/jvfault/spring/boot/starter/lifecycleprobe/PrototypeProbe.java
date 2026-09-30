package com.jvfault.spring.boot.starter.lifecycleprobe;

/**
 * 临时探针：jvfault 的 PROTOTYPE 组件，验证暴露到 Spring 后不会被压平成单例。
 */
@com.jvfault.core.annotation.Component(scope = com.jvfault.core.annotation.Component.Scope.PROTOTYPE)
public class PrototypeProbe {

    public static int instances;

    public PrototypeProbe() {
        instances++;
    }

    public static void reset() {
        instances = 0;
    }
}
