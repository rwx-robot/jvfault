package com.jvfault.transport.rmq;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 传输服务端（amqp-client 真实实现）。
 *
 * <p>语义：topic exchange（{@code jvfault.topic}）上按 pattern 做 routing-key 绑定；
 * 每个服务端实例独占一条队列（{@code jvfault.srv.<uuid>}，绑定所有已订阅 pattern），
 * 收到消息后由 {@link AbstractTransport#dispatch(Message)} 选出首个非空响应的处理器；
 * 若请求携带 {@code replyTo}，响应按 {@code correlationId} 回程，handler 异常经 error 头回传。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class RabbitMQTransportServer extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTransportServer.class);
    static final String EXCHANGE = "jvfault.topic";

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private Connection connection;
    private Channel consumeChannel;
    private Channel publishChannel;
    private String serverQueue;

    public RabbitMQTransportServer(ConnectionConfig config) {
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
            ConnectionFactory factory = new ConnectionFactory();
            try {
                factory.setUri(config.getBootstrap());
            } catch (Exception e) {
                throw new TransportException("RabbitMQ URI 非法: " + config.getBootstrap(), e);
            }
            factory.setConnectionTimeout(10_000);
            factory.setHandshakeTimeout(10_000);
            factory.setAutomaticRecoveryEnabled(true);
            connection = factory.newConnection();
            consumeChannel = connection.createChannel();
            publishChannel = connection.createChannel();
            consumeChannel.exchangeDeclare(EXCHANGE, "topic", false, true, null);
            serverQueue = "jvfault.srv." + UUID.randomUUID();
            consumeChannel.queueDeclare(serverQueue, false, false, true, null);
            for (String pattern : handlers.keySet()) {
                consumeChannel.queueBind(serverQueue, EXCHANGE, pattern);
            }
            consumeChannel.basicConsume(serverQueue, false, new DefaultConsumer(consumeChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) {
                    long tag = envelope.getDeliveryTag();
                    try {
                        RabbitMQWire wire = codec.decode(body, RabbitMQWire.class);
                        Message request = wire.toMessage();
                        Message response;
                        try {
                            response = dispatch(request);
                        } catch (Exception e) {
                            if (properties.getReplyTo() != null) {
                                publishReply(properties.getReplyTo(), properties.getCorrelationId(),
                                        new Message(request.getPattern(), new byte[0], request.getCorrelationId(),
                                                Collections.singletonMap(Message.HEADER_ERROR,
                                                        String.valueOf(e.getMessage()))));
                            }
                            consumeChannel.basicAck(tag, false);
                            return;
                        }
                        if (response != null && properties.getReplyTo() != null) {
                            publishReply(properties.getReplyTo(), properties.getCorrelationId(), response);
                        }
                        consumeChannel.basicAck(tag, false);
                    } catch (Exception e) {
                        log.warn("rabbitmq 消息处理失败: {}", e.getMessage());
                        try {
                            consumeChannel.basicNack(tag, false, true);
                        } catch (Exception ignore) {
                            // best-effort
                        }
                    }
                }
            });
            running = true;
            log.info("rabbitmq transport bound: {}", config.getBootstrap());
        } catch (IOException | TimeoutException e) {
            throw new TransportException("RabbitMQ bind 失败: " + config.getBootstrap(), e);
        }
    }

    private synchronized void publishReply(String replyTo, String correlationId, Message message) {
        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(correlationId).build();
            publishChannel.basicPublish("", replyTo, props, codec.encode(RabbitMQWire.from(message)));
        } catch (IOException e) {
            throw new TransportException("RabbitMQ 回包失败", e);
        }
    }

    @Override
    public void subscribe(String pattern, com.jvfault.microservices.MessageHandler handler) {
        super.subscribe(pattern, handler);
        if (running && consumeChannel != null) {
            try {
                consumeChannel.queueBind(serverQueue, EXCHANGE, pattern);
            } catch (IOException e) {
                throw new TransportException("RabbitMQ 订阅失败: " + pattern, e);
            }
        }
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new TransportException("rabbitmq 服务端不支持本地 request（AMQP 无请求语义）");
    }

    @Override
    public synchronized void publish(Message message) {
        if (!running) {
            bind();
        }
        try {
            publishChannel.basicPublish(EXCHANGE, message.getPattern(), null,
                    codec.encode(RabbitMQWire.from(message)));
        } catch (IOException e) {
            throw new TransportException("RabbitMQ 发布失败: " + message.getPattern(), e);
        }
    }

    @Override
    public void close() {
        running = false;
        if (consumeChannel != null) {
            try {
                consumeChannel.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
        if (publishChannel != null) {
            try {
                publishChannel.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
    }
}
