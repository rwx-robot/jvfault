package com.jvfault.transport.nats;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * NATS 传输服务端（jnats 真实实现）。
 *
 * <p>语义：subscribe 的 pattern 作为 NATS subject 订阅（{@code #} 通配翻译为 NATS 的 {@code >}）；
 * 收到消息后由 {@link AbstractTransport#dispatch(Message)} 选出首个非空响应的处理器；
 * 若请求携带 reply subject（NATS core request-reply 的 inbox），响应按原 subject 回程，
 * handler 异常经 error 头回传。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class NATSTransportServer extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(NATSTransportServer.class);

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private Connection connection;
    private Dispatcher dispatcher;

    public NATSTransportServer(ConnectionConfig config) {
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
        try {
            connection = Nats.connect(config.getBootstrap());
            dispatcher = connection.createDispatcher(this::onMessage);
            for (String pattern : handlers.keySet()) {
                dispatcher.subscribe(toNatsSubject(pattern));
            }
            running = true;
            log.info("nats transport bound: {}", config.getBootstrap());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("NATS bind 失败: " + config.getBootstrap(), e);
        } catch (IOException e) {
            throw new TransportException("NATS bind 失败: " + config.getBootstrap(), e);
        }
    }

    private void onMessage(io.nats.client.Message msg) {
        try {
            NATSWire wire = codec.decode(msg.getData(), NATSWire.class);
            Message request = wire.toMessage();
            Message response;
            try {
                response = dispatch(request);
            } catch (Exception e) {
                if (msg.getReplyTo() != null) {
                    connection.publish(msg.getReplyTo(), codec.encode(NATSWire.from(
                            new Message(request.getPattern(), new byte[0], request.getCorrelationId(),
                                    Collections.singletonMap(Message.HEADER_ERROR,
                                            String.valueOf(e.getMessage()))))));
                }
                return;
            }
            if (response != null && msg.getReplyTo() != null) {
                connection.publish(msg.getReplyTo(), codec.encode(NATSWire.from(response)));
            }
        } catch (Exception e) {
            log.warn("nats 消息处理失败: {}", e.getMessage());
        }
    }

    /** jvfault 的多段通配 {@code #} 对应 NATS 的尾部通配 {@code >}。 */
    static String toNatsSubject(String pattern) {
        return pattern.replace("#", ">");
    }

    @Override
    public void subscribe(String pattern, com.jvfault.microservices.MessageHandler handler) {
        super.subscribe(pattern, handler);
        if (running && dispatcher != null) {
            dispatcher.subscribe(toNatsSubject(pattern));
        }
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new TransportException("nats 服务端不支持本地 request（请使用对应 Client）");
    }

    @Override
    public void publish(Message message) {
        if (!running) {
            bind();
        }
        connection.publish(message.getPattern(), codec.encode(NATSWire.from(message)));
    }

    @Override
    public void close() {
        running = false;
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
    }
}
