package com.jvfault.microservices;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息信封。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class Message {

    /** 附加头：错误标记 */
    public static final String HEADER_ERROR = "x-error";

    private final String id;
    private final String pattern;
    private final byte[] data;
    private final Map<String, String> headers;
    private final String correlationId;
    private final long timestamp;

    public Message(String pattern, byte[] data) {
        this(pattern, data, null, null);
    }

    public Message(String pattern, byte[] data, String correlationId, Map<String, String> headers) {
        this.id = UUID.randomUUID().toString();
        this.pattern = pattern;
        this.data = data != null ? data.clone() : new byte[0];
        this.correlationId = correlationId;
        this.headers = headers != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(headers))
                : Collections.emptyMap();
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getPattern() { return pattern; }
    public byte[] getData() { return data.clone(); }
    public Map<String, String> getHeaders() { return headers; }
    public String getCorrelationId() { return correlationId; }
    public long getTimestamp() { return timestamp; }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Message withHeader(String name, String value) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.put(name, value);
        return new Message(pattern, data, correlationId, merged);
    }

    public Message withCorrelationId(String correlationId) {
        return new Message(pattern, data, correlationId, headers);
    }

    @Override
    public String toString() {
        return "Message{" + pattern + ", id=" + id + ", bytes=" + data.length + "}";
    }
}
