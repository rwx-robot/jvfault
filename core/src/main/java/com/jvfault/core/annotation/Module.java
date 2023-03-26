package com.jvfault.core.annotation;

import java.lang.annotation.*;

/**
 * 定义一个模块，封装相关的组件、控制器、导入模块等。
 * 对应 Spring: @Configuration + @ComponentScan
 * 
 * <p>模块是 jvfault 的基本组织单元，支持：
 * <ul>
 *   <li>providers: 该模块提供的组件 (Service, Repository 等)</li>
 *   <li>controllers: 该模块的控制器 (Web 入口)</li>
 *   <li>imports: 导入其他模块，自动传递其导出的 providers</li>
 *   <li>exports: 导出的 providers，供导入该模块的其他模块使用</li>
 * </ul>
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Module {

    /**
     * 该模块提供的组件类数组
     * 这些组件会被容器实例化并注册到模块上下文
     */
    Class<?>[] providers() default {};

    /**
     * 该模块的控制器类数组
     * 控制器处理 HTTP 请求 (需要 jvfault-web 模块)
     */
    Class<?>[] controllers() default {};

    /**
     * 导入的其他模块类数组
     * 导入的模块会被先初始化，其 exports 会合并到当前模块
     */
    Class<?>[] imports() default {};

    /**
     * 导出的组件类数组
     * 只有显式导出的 providers 才能被导入该模块的其他模块注入
     */
    Class<?>[] exports() default {};

    /**
     * 是否为全局模块
     * 全局模块的 providers 无需显式导入即可在任何地方注入
     */
    boolean global() default false;
}