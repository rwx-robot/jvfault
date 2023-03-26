package com.jvfault.transport.mqtt;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.Message;

/**
 * MQTT 传输服务端（bind 时校验配置；真实连接由客户端库建立）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class MQTTTransportServer extends AbstractTransport {

    protected final ConnectionConfig config;

    public MQTTTransportServer(ConnectionConfig config) {
        this.config = config;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    @Override
    public void bind() {
        config.validate();
        super.bind();
        org.slf4j.LoggerFactory.getLogger(MQTTTransportServer.class)
                .info("MQTT transport bound ({})", config.getBootstrap());
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new com.jvfault.microservices.TransportException("MQTT 无同步 request 语义，请使用对应 Client");
    }

    @Override
    public void publish(Message message) {
        if (!running) {
            bind();
        }
        dispatch(message);
    }

    @Override
    public void close() {
        running = false;
    }
}
