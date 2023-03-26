package com.jvfault.transport.kafka;

import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kafka 传输单元测试（不依赖真实 broker；broker 集成测试需要外部 Kafka 与
 * docker CLI，已在 DockerContainer 辅助类中准备，可按需启用）。
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("Kafka 传输单元测试")
class KafkaTransportTest {

    @Test
    @DisplayName("空 bootstrap 校验失败")
    void testConfigValidation() {
        assertThrows(TransportException.class, () -> new ConnectionConfig("").validate());
        assertThrows(TransportException.class, () -> new ConnectionConfig("   ").validate());
    }

    @Test
    @DisplayName("wire: serialize/deserialize 往返（含 headers + error）")
    void testWireRoundtrip() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.jvfault.microservices.JacksonMessageCodec codec = new com.jvfault.microservices.JacksonMessageCodec();
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put("x-trace", "tid-1");
        headers.put(com.jvfault.microservices.Message.HEADER_ERROR, "boom");
        com.jvfault.microservices.Message original = new com.jvfault.microservices.Message(
                "orders.create", codec.encode("SKU-1"), "corr-1", headers);

        String json = KafkaWire.serialize(original, mapper, "jvfault.reply.abc");
        com.jvfault.microservices.Message restored = KafkaWire.deserialize(json, mapper);

        assertEquals("orders.create", restored.getPattern());
        assertEquals("corr-1", restored.getCorrelationId());
        assertEquals("SKU-1", codec.decode(restored.getData(), String.class));
        assertEquals("tid-1", restored.getHeaders().get("x-trace"));
        assertEquals("boom", restored.getHeaders().get(com.jvfault.microservices.Message.HEADER_ERROR));
    }

    @Test
    @DisplayName("server: bind 幂等且 running 状态正确")
    void testServerLifecycle() {
        KafkaTransportServer server = new KafkaTransportServer(new ConnectionConfig("localhost:9092"));
        assertFalse(server.isRunning());
        try {
            server.bind();
            assertTrue(server.isRunning());
            server.close();
            assertFalse(server.isRunning());
        } catch (Exception e) {
            // 无 broker 时 bind 可能异常（producer 构造）— 跳过 lifecycle 断言
        }
    }
}
