package com.jvfault.microservices.annotation;

import java.lang.annotation.*;

/**
 * 请求-响应消息处理器。方法首参数类型 = 消息 payload 反序列化类型，
 * 可选第二参数 Message；非 void 返回值自动作为响应回发。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {

    /** 订阅 pattern（支持 * 与 # 通配） */
    String value();
}
