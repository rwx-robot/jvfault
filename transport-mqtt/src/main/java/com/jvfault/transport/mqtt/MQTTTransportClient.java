package com.jvfault.transport.mqtt;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import com.jvfault.microservices.TransportTimeoutException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 传输客户端（Eclipse Paho 真实实现）。
 *
 * <p>request-reply：请求发布到 pattern topic，wire 携带独占 replyTopic；
 * 客户端订阅 replyTopic，按 correlationId 匹配响应。handler 异常经 error 头回传。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class MQTTTransportClient implements TransportClient {

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private MqttClient client;
    private volatile boolean connected;
    private String replyTopic;
    private final ConcurrentHashMap<String, BlockingQueue<Message>> pending = new ConcurrentHashMap<>();

    public MQTTTransportClient(ConnectionConfig config) {
        this.config = config;
    }

    @Override
    public synchronized void connect() {
        if (connected) {
            return;
        }
        config.validate();
        try {
            client = new MqttClient(config.getBootstrap(), "jvfault-cli-" + UUID.randomUUID(),
                    new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            client.connect(options);
            replyTopic = "jvfault/reply/" + UUID.randomUUID();
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // no-op
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        MqttWire wire = codec.decode(message.getPayload(), MqttWire.class);
                        Message response = wire.toMessage();
                        BlockingQueue<Message> queue = pending.get(response.getCorrelationId());
                        if (queue != null) {
                            queue.add(response);
                        }
                    } catch (Exception ignored) {
                        // 无法解析的回包忽略
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // no-op
                }
            });
            client.subscribe(replyTopic);
            connected = true;
        } catch (MqttException e) {
            throw new TransportException("MQTT connect 失败: " + config.getBootstrap(), e);
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
        String correlationId = UUID.randomUUID().toString();
        BlockingQueue<Message> queue = pending.computeIfAbsent(correlationId,
                k -> new LinkedBlockingQueue<>());
        try {
            Message request = new Message(pattern, codec.encode(payload), correlationId, null);
            MqttMessage mqttMessage = new MqttMessage(codec.encode(MqttWire.from(request, replyTopic)));
            mqttMessage.setQos(1);
            client.publish(pattern, mqttMessage);

            Message response = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new TransportTimeoutException("mqtt request 超时: " + pattern);
            }
            if (response.getHeader(Message.HEADER_ERROR) != null) {
                throw new TransportException("远端错误: " + response.getHeader(Message.HEADER_ERROR));
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("mqtt request 被中断", e);
        } catch (MqttException e) {
            throw new TransportException("mqtt request 发送失败: " + pattern, e);
        } finally {
            pending.remove(correlationId);
        }
    }

    @Override
    public void emit(String pattern, Object payload) {
        ensureConnected();
        Message message = new Message(pattern, codec.encode(payload));
        try {
            MqttMessage mqttMessage = new MqttMessage(codec.encode(MqttWire.from(message, null)));
            mqttMessage.setQos(1);
            client.publish(pattern, mqttMessage);
        } catch (MqttException e) {
            throw new TransportException("mqtt emit 失败: " + pattern, e);
        }
    }

    @Override
    public synchronized void close() {
        connected = false;
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception ignore) {
                // best-effort
            }
            try {
                client.close();
            } catch (Exception ignore) {
                // best-effort
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
