package com.jvfault.transport.nats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NATS 连接配置（声明式，连接在 bind/connect 时建立）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class ConnectionConfig {

    private final Map<String, String> options = new LinkedHashMap<>();

    public ConnectionConfig(String bootstrap) {
        options.put("bootstrap", bootstrap);
    }

    public ConnectionConfig option(String key, String value) {
        options.put(key, value);
        return this;
    }

    public String getBootstrap() {
        return options.get("bootstrap");
    }

    public String getOption(String key, String def) {
        return options.getOrDefault(key, def);
    }

    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(options);
    }

    void validate() {
        if (getBootstrap() == null || getBootstrap().trim().isEmpty()) {
            throw new com.jvfault.microservices.TransportException("NATS bootstrap 地址不能为空");
        }
    }
}
