package com.jvfault.websocket.annotation;

import java.lang.annotation.*;

/**
 * 连接断开回调（方法参数可注入 WebSocketSession）。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnDisconnect {
}
