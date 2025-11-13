package com.jvfault.microservices;

/**
 * 消息编解码 SPI。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public interface MessageCodec {

    byte[] encode(Object value);

    <T> T decode(byte[] data, Class<T> targetType);
}
