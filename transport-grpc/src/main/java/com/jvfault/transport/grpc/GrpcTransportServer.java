package com.jvfault.transport.grpc;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportException;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * gRPC 传输服务端（grpc-netty 真实实现）。
 *
 * <p>单个 unary 方法 {@code jvfault.transport.JvfaultTransport/Invoke} 承载全部 pattern：
 * 请求体为 JSON wire，服务端还原为 {@link Message} 后由
 * {@link AbstractTransport#dispatch(Message)} 选出首个非空响应的处理器；
 * handler 异常经 error 头回传（作为正常响应体，客户端解码后抛 {@link TransportException}）。
 *
 * <p>bootstrap 形如 {@code host:port}；port 为 0 时使用随机端口（测试友好），
 * 实际端口通过 {@link #getPort()} 获取。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class GrpcTransportServer extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(GrpcTransportServer.class);

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private Server server;

    public GrpcTransportServer(ConnectionConfig config) {
        this.config = config;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    @Override
    public void bind() {
        if (running) {
            return;
        }
        config.validate();
        int port = GrpcMethod.port(config.getBootstrap());
        ServerServiceDefinition service = ServerServiceDefinition.builder(GrpcMethod.SERVICE_NAME)
                .addMethod(GrpcMethod.METHOD, ServerCalls.asyncUnaryCall(
                        new ServerCalls.UnaryMethod<byte[], byte[]>() {
                            @Override
                            public void invoke(byte[] request, StreamObserver<byte[]> observer) {
                                handleInvoke(request, observer);
                            }
                        }))
                .build();
        try {
            server = NettyServerBuilder.forPort(port).addService(service).build().start();
            running = true;
            log.info("grpc transport bound: {} (port={})", config.getBootstrap(), server.getPort());
        } catch (IOException e) {
            throw new TransportException("gRPC bind 失败: " + config.getBootstrap(), e);
        }
    }

    private void handleInvoke(byte[] requestBytes, StreamObserver<byte[]> observer) {
        try {
            GrpcWire wire = codec.decode(requestBytes, GrpcWire.class);
            Message request = wire.toMessage();
            Message response;
            try {
                response = dispatch(request);
            } catch (Exception e) {
                observer.onNext(codec.encode(GrpcWire.from(new Message(request.getPattern(), new byte[0],
                        request.getCorrelationId(),
                        Collections.singletonMap(Message.HEADER_ERROR, String.valueOf(e.getMessage()))))));
                observer.onCompleted();
                return;
            }
            Message payload = response != null
                    ? response
                    : new Message(request.getPattern(), new byte[0], request.getCorrelationId(), null);
            observer.onNext(codec.encode(GrpcWire.from(payload)));
            observer.onCompleted();
        } catch (Exception e) {
            log.warn("grpc 调用处理失败: {}", e.getMessage());
            observer.onError(e);
        }
    }

    /** 服务端实际监听端口（未 bind 返回 -1）。 */
    public int getPort() {
        return server != null ? server.getPort() : -1;
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new TransportException("gRPC 服务端不支持本地 request（请使用对应 Client）");
    }

    @Override
    public void publish(Message message) {
        if (!running) {
            bind();
        }
        // gRPC 为点对点调用，无 broker 广播语义：此处按本地分发处理（仅供事件派发场景）。
        dispatch(message);
    }

    @Override
    public void close() {
        running = false;
        if (server != null) {
            server.shutdownNow();
        }
    }
}
