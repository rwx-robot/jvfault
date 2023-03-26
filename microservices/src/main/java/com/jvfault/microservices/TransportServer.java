package com.jvfault.microservices;

/**
 * 传输服务端 SPI。各具体传输（TCP/gRPC/Kafka/...）实现本接口。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public interface TransportServer {

    /** 绑定并开始监听 */
    void bind();

    /**
     * 订阅 pattern 的消息（request-reply 或事件）。
     */
    void subscribe(String pattern, MessageHandler handler);

    /**
     * 发布事件消息。
     */
    void publish(Message message);

    /**
     * 请求-响应语义：发送并等待响应。
     */
    Message request(Message request, long timeoutMillis);

    void close();

    boolean isRunning();
}
