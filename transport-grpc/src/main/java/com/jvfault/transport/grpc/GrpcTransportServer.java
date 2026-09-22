package com.jvfault.transport.grpc;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.Message;

/**
 * gRPC 传输服务端（bind 时校验配置；真实 server 由 grpc ServerBuilder 构建）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class GrpcTransportServer extends AbstractTransport {

    protected final ConnectionConfig config;

    public GrpcTransportServer(ConnectionConfig config) {
        this.config = config;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    @Override
    public void bind() {
        config.validate();
        super.bind();
        org.slf4j.LoggerFactory.getLogger(GrpcTransportServer.class)
                .info("gRPC transport bound ({})", config.getBootstrap());
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new com.jvfault.microservices.TransportException("gRPC 无同步 request 语义，请使用对应 Client");
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
