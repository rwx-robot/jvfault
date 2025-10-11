package com.jvfault.transport.mqtt;

import com.jvfault.microservices.Message;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * MQTT wire 格式（JSON 载荷，与 redis/kafka/rmq/nats 同构）。
 *
 * <p>MQTT 无原生 replyTo，故 replyTopic 随 wire 传递；
 * 响应按 correlationId 在 replyTopic 上匹配。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class MqttWire {

    private String pattern;
    private String dataBase64;
    private String correlationId;
    private String replyTopic;
    private String error;

    public static MqttWire from(Message message, String replyTopic) {
        MqttWire wire = new MqttWire();
        wire.pattern = message.getPattern();
        wire.dataBase64 = Base64.getEncoder().encodeToString(message.getData());
        wire.correlationId = message.getCorrelationId();
        wire.replyTopic = replyTopic;
        wire.error = message.getHeader(Message.HEADER_ERROR);
        return wire;
    }

    public Message toMessage() {
        byte[] data = dataBase64 != null ? Base64.getDecoder().decode(dataBase64) : new byte[0];
        Map<String, String> headers = error != null
                ? Collections.singletonMap(Message.HEADER_ERROR, error) : null;
        return new Message(pattern, data, correlationId, headers);
    }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getDataBase64() { return dataBase64; }
    public void setDataBase64(String dataBase64) { this.dataBase64 = dataBase64; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getReplyTopic() { return replyTopic; }
    public void setReplyTopic(String replyTopic) { this.replyTopic = replyTopic; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
