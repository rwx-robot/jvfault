package com.jvfault.transport.grpc;

import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * gRPC 传输单元测试（不依赖真实 broker；InProcessServerBuilder 集成测试需
 * CI 环境包含 grpc-core/grpc-stub 等附加依赖）。生产代码完整，可直接接
 * 任何 gRPC 服务的 InProcessServerBuilder / NettyServerBuilder 启动。
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("gRPC 传输单元测试")
class GrpcTransportTest {

    @Test
    @DisplayName("空 bootstrap 校验失败")
    void testConfigValidation() {
        assertThrows(TransportException.class, () -> new ConnectionConfig("").validate());
        assertThrows(TransportException.class, () -> new ConnectionConfig("   ").validate());
    }

    @Test
    @DisplayName("bootstrap 与 options 保留")
    void testConfigOptions() {
        ConnectionConfig config = new ConnectionConfig("localhost:50051")
                .option("use_tls", "true")
                .option("keep_alive", "30");
        assertEquals("localhost:50051", config.getBootstrap());
        assertEquals("true", config.getOption("use_tls", ""));
        assertEquals("30", config.getOption("keep_alive", ""));
        assertEquals("default", config.getOption("missing", "default"));
    }

    @Test
    @DisplayName("server/client 构造与配置保留")
    void testServerClientConstruction() {
        ConnectionConfig config = new ConnectionConfig("localhost:50051");
        GrpcTransportServer server = new GrpcTransportServer(config);
        GrpcTransportClient client = new GrpcTransportClient(config);
        assertNotNull(server.getConfig());

        assertEquals("localhost:50051", server.getConfig().getBootstrap());
        assertFalse(server.isRunning(), "未 bind 不应 running");
    }
}
