package com.jvfault.web.annotation;

import java.lang.annotation.*;

/**
 * 请求体绑定（JSON 反序列化为参数类型）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Body {
}
