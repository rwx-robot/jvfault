package com.jvfault.transport.nats;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import com.jvfault.microservices.TransportTimeoutException;
import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NATS 传输客户端（jnats 真实实现）。
 *
 * <p>request-reply 直接复用 NATS core 的 request/response（库自动创建 inbox reply subject）；
 * handler 异常经 error 头回传，解码后抛 {@link TransportException}。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class NATSTransportClient implements TransportClient {

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private Connection connection;
    private volatile boolean connected;

    public NATSTransportClient(ConnectionConfig config) {
        this.config = config;
    }

    @Override
    public synchronized void connect() {
        if (connected) {
            return;
        }
        config.validate();
        try {
            connection = Nats.connect(config.getBootstrap());
            connected = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("NATS connect 失败: " + config.getBootstrap(), e);
        } catch (IOException e) {
            throw new TransportException("NATS connect 失败: " + config.getBootstrap(), e);
        }
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
        CompletableFuture<io.nats.client.Message> future =
                connection.request(pattern, codec.encode(NATSWire.from(request)));
        try {
            io.nats.client.Message reply = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (reply == null) {
                throw new TransportTimeoutException("nats request 超时: " + pattern);
            }
            Message response = codec.decode(reply.getData(), NATSWire.class).toMessage();
            if (response.getHeader(Message.HEADER_ERROR) != null) {
                throw new TransportException("远端错误: " + response.getHeader(Message.HEADER_ERROR));
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("nats request 被中断", e);
        } catch (TimeoutException e) {
            throw new TransportTimeoutException("nats request 超时: " + pattern);
        } catch (ExecutionException e) {
            throw new TransportException("nats request 失败: " + pattern, e.getCause());
        }
    }

    @Override
    public void emit(String pattern, Object payload) {
        ensureConnected();
        Message message = new Message(pattern, codec.encode(payload));
        connection.publish(pattern, codec.encode(NATSWire.from(message)));
    }

    @Override
    public synchronized void close() {
        connected = false;
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
