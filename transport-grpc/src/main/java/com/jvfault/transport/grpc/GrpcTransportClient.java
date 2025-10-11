package com.jvfault.transport.grpc;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import com.jvfault.microservices.TransportTimeoutException;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCalls;

import java.util.concurrent.TimeUnit;

/**
 * gRPC 传输客户端（grpc-netty 真实实现）。
 *
 * <p>request-reply 走 {@link ClientCalls#blockingUnaryCall} 到通用 unary 方法；
 * 请求/响应体为 JSON wire；handler 异常经 error 头回传后抛 {@link TransportException}。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class GrpcTransportClient implements TransportClient {

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private ManagedChannel channel;
    private volatile boolean connected;

    public GrpcTransportClient(ConnectionConfig config) {
        this.config = config;
    }

    @Override
    public synchronized void connect() {
        if (connected) {
            return;
        }
        config.validate();
        channel = NettyChannelBuilder
                .forAddress(GrpcMethod.host(config.getBootstrap()), GrpcMethod.port(config.getBootstrap()))
                .usePlaintext()
                .build();
        connected = true;
    }

    private void ensureConnected() {
        if (!connected) {
            connect();
        }
    }

    @Override
    public Message request(String pattern, Object payload, long timeoutMillis) {
        ensureConnected();
        Message request = new Message(pattern, codec.encode(payload));
        try {
            byte[] responseBytes = ClientCalls.blockingUnaryCall(channel, GrpcMethod.METHOD,
                    CallOptions.DEFAULT.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS),
                    codec.encode(GrpcWire.from(request)));
            Message response = codec.decode(responseBytes, GrpcWire.class).toMessage();
            if (response.getHeader(Message.HEADER_ERROR) != null) {
                throw new TransportException("远端错误: " + response.getHeader(Message.HEADER_ERROR));
            }
            return response;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new TransportTimeoutException("gRPC request 超时: " + pattern);
            }
            throw new TransportException("gRPC request 失败: " + pattern, e);
        }
    }

    @Override
    public void emit(String pattern, Object payload) {
        ensureConnected();
        Message message = new Message(pattern, codec.encode(payload));
        try {
            ClientCalls.blockingUnaryCall(channel, GrpcMethod.METHOD,
                    CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS),
                    codec.encode(GrpcWire.from(message)));
        } catch (StatusRuntimeException e) {
            throw new TransportException("gRPC emit 失败: " + pattern, e);
        }
    }

    @Override
    public synchronized void close() {
        connected = false;
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
