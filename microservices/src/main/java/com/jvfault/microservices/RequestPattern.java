package com.jvfault.microservices;

import java.lang.annotation.*;

/**
 * 客户端接口方法的自定义 pattern。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPattern {

    String value();
}
