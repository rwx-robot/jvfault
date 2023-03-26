package com.jvfault.transport.redis;

import com.jvfault.microservices.Message;

import java.util.Base64;
import java.util.Map;

/**
 * Redis Pub/Sub wire 格式（JSON）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class RedisWire {

    private String pattern;
    private String dataBase64;
    private String correlationId;
    private String replyChannel;
    private String error;

    public static RedisWire from(Message message, String replyChannel) {
        RedisWire wire = new RedisWire();
        wire.pattern = message.getPattern();
        wire.dataBase64 = Base64.getEncoder().encodeToString(message.getData());
        wire.correlationId = message.getCorrelationId();
        wire.replyChannel = replyChannel;
        wire.error = message.getHeader(Message.HEADER_ERROR);
        return wire;
    }

    public Message toMessage() {
        byte[] data = dataBase64 != null ? Base64.getDecoder().decode(dataBase64) : new byte[0];
        Map<String, String> headers = error != null
                ? java.util.Collections.singletonMap(Message.HEADER_ERROR, error) : null;
        return new Message(pattern, data, correlationId, headers);
    }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getDataBase64() { return dataBase64; }
    public void setDataBase64(String dataBase64) { this.dataBase64 = dataBase64; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getReplyChannel() { return replyChannel; }
    public void setReplyChannel(String replyChannel) { this.replyChannel = replyChannel; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
