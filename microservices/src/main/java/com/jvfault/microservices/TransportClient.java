package com.jvfault.microservices;

/**
 * 传输客户端 SPI。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public interface TransportClient {

    void connect();

    /**
     * 请求-响应。
     */
    Message request(String pattern, Object payload, long timeoutMillis);

    /**
     * 事件发射（无响应）。
     */
    void emit(String pattern, Object payload);

    void close();
}
