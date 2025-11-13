package com.jvfault.config.annotation;

import java.lang.annotation.*;

/**
 * Profile 条件 Bean：当前激活 Profile 不在 value 列表中时，
 * Bean 创建失败（fail-fast）。
 * 对应 现代框架/Spring: @Profile
 *
 * @since v0.3.0 (2017)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Profile {

    /** 匹配任一即生效 */
    String[] value();
}
