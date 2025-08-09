package com.jvfault.aop.annotation;

import java.lang.annotation.*;

/**
 * 标记一个类为切面。切面类中的通知方法（@Around/@Before/@After 等）
 * 依 pointcut 表达式织入目标 Bean。
 * 对应 AspectJ: @Aspect
 *
 * <p>注意：切面 Bean 需先于目标 Bean 注册（例如放入全局模块），
 * 否则晚于目标创建的切面不会对已代理的 Bean 生效。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Aspect {
}
