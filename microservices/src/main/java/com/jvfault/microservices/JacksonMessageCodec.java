package com.jvfault.microservices;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 消息编解码器（默认实现）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class JacksonMessageCodec implements MessageCodec {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] encode(Object value) {
        if (value == null) {
            return new byte[0];
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        try {
            return mapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new TransportException("消息编码失败: " + value.getClass(), e);
        }
    }

    @Override
    public <T> T decode(byte[] data, Class<T> targetType) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(data, targetType);
        } catch (Exception e) {
            throw new TransportException("消息解码失败 -> " + targetType.getName(), e);
        }
    }
}
