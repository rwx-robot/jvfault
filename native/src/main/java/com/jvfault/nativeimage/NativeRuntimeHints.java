package com.jvfault.nativeimage;

import com.jvfault.aot.ReflectConfigGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Native 运行时提示聚合器 - 收集需要反射注册的 Bean 类
 * 并输出 GraalVM native-image 所需配置。
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public class NativeRuntimeHints {

    private final List<Class<?>> reflectionClasses = new ArrayList<>();

    public NativeRuntimeHints register(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (!reflectionClasses.contains(clazz)) {
                reflectionClasses.add(clazz);
            }
        }
        return this;
    }

    /**
     * 生成 reflect-config.json 内容。
     */
    public String generateReflectConfig() {
        return new ReflectConfigGenerator().toJson(reflectionClasses);
    }

    public List<Class<?>> getReflectionClasses() {
        return new ArrayList<>(reflectionClasses);
    }

    /** native 模式探测（-Djvfault.native=true） */
    public static boolean isNative() {
        return Boolean.getBoolean("jvfault.native")
                || System.getenv("JVFAULT_NATIVE") != null;
    }
}
