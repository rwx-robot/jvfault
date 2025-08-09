package com.jvfault.microservices;

/**
 * 服务端消息处理器。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * 处理请求消息并返回响应（事件处理器返回 null）。
     */
    Message handle(Message request);
}
