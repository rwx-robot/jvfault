package com.jvfault.transport.mqtt;

import com.jvfault.microservices.AbstractTransport;
import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.MessageCodec;
import com.jvfault.microservices.TransportException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.UUID;

/**
 * MQTT 传输服务端（Eclipse Paho 真实实现）。
 *
 * <p>语义：subscribe 的 pattern 作为 MQTT topic filter（jvfault {@code *} 翻译为 MQTT {@code +}）；
 * 收到消息后由 {@link AbstractTransport#dispatch(Message)} 选出首个非空响应的处理器；
 * 若 wire 携带 replyTopic，响应按 topic 回程，handler 异常经 error 头回传。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public class MQTTTransportServer extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(MQTTTransportServer.class);

    private final ConnectionConfig config;
    private final MessageCodec codec = new JacksonMessageCodec();
    private MqttClient client;

    public MQTTTransportServer(ConnectionConfig config) {
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
            client = new MqttClient(config.getBootstrap(), "jvfault-srv-" + UUID.randomUUID(),
                    new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            client.connect(options);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("mqtt 连接断开: {}", cause == null ? "unknown" : cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    onMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // no-op
                }
            });
            for (String pattern : handlers.keySet()) {
                client.subscribe(toMqttTopic(pattern));
            }
            running = true;
            log.info("mqtt transport bound: {}", config.getBootstrap());
        } catch (MqttException e) {
            throw new TransportException("MQTT bind 失败: " + config.getBootstrap(), e);
        }
    }

    private void onMessage(String topic, MqttMessage message) {
        try {
            MqttWire wire = codec.decode(message.getPayload(), MqttWire.class);
            Message request = wire.toMessage();
            Message response;
            try {
                response = dispatch(request);
            } catch (Exception e) {
                if (wire.getReplyTopic() != null) {
                    publishTo(wire.getReplyTopic(), new Message(request.getPattern(), new byte[0],
                            request.getCorrelationId(),
                            Collections.singletonMap(Message.HEADER_ERROR, String.valueOf(e.getMessage()))));
                }
                return;
            }
            if (response != null && wire.getReplyTopic() != null) {
                publishTo(wire.getReplyTopic(), response);
            }
        } catch (Exception e) {
            log.warn("mqtt 消息处理失败: {}", e.getMessage());
        }
    }

    private void publishTo(String topic, Message message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(codec.encode(MqttWire.from(message, null)));
        mqttMessage.setQos(1);
        client.publish(topic, mqttMessage);
    }

    /** jvfault 的单段通配 {@code *} 对应 MQTT 的 {@code +}（{@code #} 语义一致）。 */
    static String toMqttTopic(String pattern) {
        return pattern.replace("*", "+");
    }

    @Override
    public void subscribe(String pattern, com.jvfault.microservices.MessageHandler handler) {
        super.subscribe(pattern, handler);
        if (running && client != null) {
            try {
                client.subscribe(toMqttTopic(pattern));
            } catch (MqttException e) {
                throw new TransportException("MQTT 订阅失败: " + pattern, e);
            }
        }
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        throw new TransportException("mqtt 服务端不支持本地 request（请使用对应 Client）");
    }

    @Override
    public void publish(Message message) {
        if (!running) {
            bind();
        }
        try {
            publishTo(message.getPattern(), message);
        } catch (MqttException e) {
            throw new TransportException("MQTT 发布失败: " + message.getPattern(), e);
        }
    }

    @Override
    public void close() {
        running = false;
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
}
