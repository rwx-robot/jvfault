package com.jvfault.transport.grpc;

import com.jvfault.microservices.Message;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * gRPC wire 格式（JSON 载荷，与其它传输同构）。
 *
 * <p>gRPC 为点对点调用，无 replyTo 概念；wire body 仅承载
 * pattern / data / correlationId / error。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class GrpcWire {

    private String pattern;
    private String dataBase64;
    private String correlationId;
    private String error;

    public static GrpcWire from(Message message) {
        GrpcWire wire = new GrpcWire();
        wire.pattern = message.getPattern();
        wire.dataBase64 = Base64.getEncoder().encodeToString(message.getData());
        wire.correlationId = message.getCorrelationId();
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
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
