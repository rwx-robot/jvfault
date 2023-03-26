package com.jvfault.websocket.annotation;

import java.lang.annotation.*;

/**
 * 订阅 WebSocket 事件。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeMessage {

    /** 事件名 */
    String value();
}
