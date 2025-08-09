package com.jvfault.websocket.annotation;

import java.lang.annotation.*;

/**
 * WebSocket 网关。value 为连接路径。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebSocketGateway {

    /** 监听路径 */
    String value() default "/";
}
