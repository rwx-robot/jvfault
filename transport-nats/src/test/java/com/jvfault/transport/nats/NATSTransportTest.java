package com.jvfault.transport.nats;

import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NATS 传输单元测试（不依赖真实 broker；broker 集成测试脚本在源码注释说明）。
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("NATS 传输单元测试")
class NATSTransportTest {

    @Test
    @DisplayName("空 bootstrap 校验失败")
    void testConfigValidation() {
        assertThrows(TransportException.class, () -> new ConnectionConfig("").validate());
        assertThrows(TransportException.class, () -> new ConnectionConfig("   ").validate());
    }

    @Test
    @DisplayName("server/client 构造与配置保留")
    void testLifecycle() {
        ConnectionConfig config = new ConnectionConfig("localhost:5672")
                .option("user", "admin")
                .option("virtualHost", "/");
        NATSTransportServer server = new NATSTransportServer(config);
        NATSTransportClient client = new NATSTransportClient(config);
        assertNotNull(server.getConfig());
        assertEquals("localhost:5672", server.getConfig().getBootstrap());
        assertEquals("admin", server.getConfig().getOption("user", ""));
        assertFalse(server.isRunning(), "未 bind 不应 running");
    }

    @Test
    @DisplayName("wire 字段正确（pattern / id / data）")
    void testWireFields() throws Exception {
        com.jvfault.microservices.JacksonMessageCodec codec = new com.jvfault.microservices.JacksonMessageCodec();
        com.jvfault.microservices.Message original = new com.jvfault.microservices.Message(
                "nats.hello", codec.encode("ping"), "corr-1", null);
        assertEquals("nats.hello", original.getPattern());
        assertEquals("corr-1", original.getCorrelationId());
        assertEquals("ping", codec.decode(original.getData(), String.class));
    }
}
