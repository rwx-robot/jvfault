package com.jvfault.core.util;

import io.github.classgraph.ClassInfo;

import java.util.function.Predicate;

/**
 * 类过滤器工具类
 * 用于 ModuleScanner 的包含/排除过滤
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public final class ClassFilter {

    private ClassFilter() {}

    /**
     * 包名前缀匹配过滤器
     */
    public static Predicate<ClassInfo> packagePrefix(String... prefixes) {
        return classInfo -> {
            String name = classInfo.getName();
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * 类名正则匹配过滤器
     */
    public static Predicate<ClassInfo> classNamePattern(String regex) {
        return classInfo -> classInfo.getName().matches(regex);
    }

    /**
     * 排除测试类
     */
    public static Predicate<ClassInfo> excludeTests() {
        return classInfo -> {
            String name = classInfo.getName();
            return !name.endsWith("Test") 
                    && !name.endsWith("Tests") 
                    && !name.contains(".test.")
                    && !name.contains(".tests.");
        };
    }

    /**
     * 仅包含公共类
     */
    public static Predicate<ClassInfo> publicOnly() {
        return ClassInfo::isPublic;
    }

    /**
     * 排除抽象类
     */
    public static Predicate<ClassInfo> excludeAbstract() {
        return classInfo -> !classInfo.isAbstract();
    }

    /**
     * 组合过滤器 (AND)
     */
    @SafeVarargs
    public static Predicate<ClassInfo> and(Predicate<ClassInfo>... filters) {
        return classInfo -> {
            for (Predicate<ClassInfo> filter : filters) {
                if (!filter.test(classInfo)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * 组合过滤器 (OR)
     */
    @SafeVarargs
    public static Predicate<ClassInfo> or(Predicate<ClassInfo>... filters) {
        return classInfo -> {
            for (Predicate<ClassInfo> filter : filters) {
                if (filter.test(classInfo)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * 取反过滤器
     */
    public static Predicate<ClassInfo> not(Predicate<ClassInfo> filter) {
        return filter.negate();
    }
}