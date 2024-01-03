package com.jvfault.transport.mqtt;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MQTT 传输集成测试（真实 broker）。
 *
 * <p>bootstrap 来源优先级：环境变量 {@code JVFAULT_MQTT_BOOTSTRAP}（CI service）
 * → 本地 docker 容器（eclipse-mosquitto:1.6）→ 不可用则跳过。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
@DisplayName("MQTT 传输集成测试（真实 broker）")
class MQTTIntegrationTest {

    private static final long TIMEOUT_MS = 20_000L;

    static DockerContainer mqtt;
    static String bootstrap;
    static boolean brokerUp;

    @BeforeAll
    static void startMqtt() throws Exception {
        String external = System.getenv("JVFAULT_MQTT_BOOTSTRAP");
        if (external != null && !external.trim().isEmpty()) {
            bootstrap = external.trim();
        } else if (DockerContainer.dockerAvailable() && DockerContainer.hasImage()) {
            mqtt = new DockerContainer().start();
            bootstrap = mqtt.getBootstrap();
        } else {
            brokerUp = false;
            return;
        }
        brokerUp = true;
    }

    @AfterAll
    static void stopMqtt() {
        if (mqtt != null) {
            mqtt.close();
        }
    }

    @Test
    @DisplayName("端到端 request-reply（replyTopic + correlationId）")
    void testRequestReply() throws Exception {
        assumeTrue(brokerUp, "需要 MQTT broker（Docker 或 JVFAULT_MQTT_BOOTSTRAP）");

        JacksonMessageCodec codec = new JacksonMessageCodec();
        MQTTTransportServer server = new MQTTTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders/create", request -> {
            String sku = codec.decode(request.getData(), String.class);
            return new Message(request.getPattern(), codec.encode("ACK:" + sku),
                    request.getCorrelationId(), null);
        });
        server.bind();

        TransportClient client = new MQTTTransportClient(new ConnectionConfig(bootstrap));
        Message response = client.request("orders/create", "SKU-1", TIMEOUT_MS);
        assertEquals("ACK:SKU-1", codec.decode(response.getData(), String.class));

        client.close();
        server.close();
    }

    @Test
    @DisplayName("handler 异常经 error 头回传客户端")
    void testErrorHeader() throws Exception {
        assumeTrue(brokerUp, "需要 MQTT broker（Docker 或 JVFAULT_MQTT_BOOTSTRAP）");

        MQTTTransportServer server = new MQTTTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders/invalid", request -> {
            throw new IllegalArgumentException("库存不足");
        });
        server.bind();

        TransportClient client = new MQTTTransportClient(new ConnectionConfig(bootstrap));
        TransportException error = assertThrows(TransportException.class,
                () -> client.request("orders/invalid", "SKU-2", TIMEOUT_MS));
        assertTrue(error.getMessage().contains("库存不足"), error.getMessage());

        client.close();
        server.close();
    }

    @Test
    @DisplayName("事件语义：publish 不等待响应")
    void testPublishEvent() throws Exception {
        assumeTrue(brokerUp, "需要 MQTT broker（Docker 或 JVFAULT_MQTT_BOOTSTRAP）");

        MQTTTransportServer server = new MQTTTransportServer(new ConnectionConfig(bootstrap));
        server.bind();
        server.publish(new Message("orders/event",
                new JacksonMessageCodec().encode("payload"), null, null));
        assertTrue(server.isRunning());

        MQTTTransportClient client = new MQTTTransportClient(new ConnectionConfig(bootstrap));
        client.emit("orders/event", "payload");
        client.close();
        server.close();
    }
}
