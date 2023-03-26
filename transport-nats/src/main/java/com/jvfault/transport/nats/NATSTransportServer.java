package com.jvfault.transport.nats;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.Message;

/**
 * NATS 传输服务端（bind 时校验配置；真实连接由客户端库建立）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class NATSTransportServer extends AbstractTransport {

    protected final ConnectionConfig config;

    public NATSTransportServer(ConnectionConfig config) {
        this.config = config;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    @Override
    public void bind() {
        config.validate();
        super.bind();
        org.slf4j.LoggerFactory.getLogger(NATSTransportServer.class)
                .info("NATS transport bound ({})", config.getBootstrap());
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new com.jvfault.microservices.TransportException("NATS 无同步 request 语义，请使用对应 Client");
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
