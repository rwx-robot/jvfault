package com.jvfault.transport.grpc;

import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;

/**
 * gRPC 传输客户端骨架：编码与连接生命周期由本类管理，
 * 真实网络发送由 doRequest/doEmit 钩子实现（子类适配器提供）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class GrpcTransportClient implements TransportClient {

    protected final ConnectionConfig config;
    protected final MessageCodec codec = new JacksonMessageCodec();
    protected volatile boolean connected;

    public GrpcTransportClient(ConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void connect() {
        config.validate();
        doConnect();
        connected = true;
    }

    protected void doConnect() {
    }

    protected void ensureConnected() {
        if (!connected) {
            connect();
        }
    }

    @Override
    public Message request(String pattern, Object payload, long timeoutMillis) {
        ensureConnected();
        byte[] data = codec.encode(payload);
        byte[] response = doRequest(pattern, data, timeoutMillis);
        return response != null ? new Message(pattern, response) : null;
    }

    protected byte[] doRequest(String pattern, byte[] data, long timeoutMillis) {
        throw new TransportException("gRPC request 需要子类适配");
    }

    @Override
    public void emit(String pattern, Object payload) {
        ensureConnected();
        doEmit(pattern, codec.encode(payload));
    }

    protected void doEmit(String pattern, byte[] data) {
        throw new TransportException("gRPC emit 需要子类适配");
    }

    @Override
    public void close() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}
