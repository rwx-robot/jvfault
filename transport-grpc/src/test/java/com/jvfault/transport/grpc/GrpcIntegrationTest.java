package com.jvfault.transport.grpc;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * gRPC 传输集成测试（真实 grpc-netty server，本机回环）。
 *
 * <p>无需 Docker：服务端绑定随机端口（bootstrap port=0），
 * 客户端按 {@link GrpcTransportServer#getPort()} 返回的实际端口连接。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
@DisplayName("gRPC 传输集成测试（真实 netty）")
class GrpcIntegrationTest {

    private static final long TIMEOUT_MS = 20_000L;

    @Test
    @DisplayName("端到端 request-reply（netty 回环）")
    void testRequestReply() throws Exception {
        JacksonMessageCodec codec = new JacksonMessageCodec();
        GrpcTransportServer server = new GrpcTransportServer(new ConnectionConfig("localhost:0"));
        server.subscribe("orders.create", request -> {
            String sku = codec.decode(request.getData(), String.class);
            return new Message(request.getPattern(), codec.encode("ACK:" + sku),
                    request.getCorrelationId(), null);
        });
        server.bind();
        try {
            TransportClient client = new GrpcTransportClient(
                    new ConnectionConfig("localhost:" + server.getPort()));
            Message response = client.request("orders.create", "SKU-1", TIMEOUT_MS);
            assertEquals("ACK:SKU-1", codec.decode(response.getData(), String.class));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    @DisplayName("handler 异常经 error 头回传客户端")
    void testErrorHeader() throws Exception {
        GrpcTransportServer server = new GrpcTransportServer(new ConnectionConfig("localhost:0"));
        server.subscribe("orders.invalid", request -> {
            throw new IllegalArgumentException("库存不足");
        });
        server.bind();
        try {
            TransportClient client = new GrpcTransportClient(
                    new ConnectionConfig("localhost:" + server.getPort()));
            TransportException error = assertThrows(TransportException.class,
                    () -> client.request("orders.invalid", "SKU-2", TIMEOUT_MS));
            assertTrue(error.getMessage().contains("库存不足"), error.getMessage());
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    @DisplayName("事件语义：emit 不阻塞等待业务响应")
    void testEmitEvent() throws Exception {
        GrpcTransportServer server = new GrpcTransportServer(new ConnectionConfig("localhost:0"));
        server.bind();
        try {
            GrpcTransportClient client = new GrpcTransportClient(
                    new ConnectionConfig("localhost:" + server.getPort()));
            client.emit("orders.event", "payload");
            assertTrue(server.isRunning());
            assertTrue(client.isConnected());
            client.close();
        } finally {
            server.close();
        }
    }
}
