package com.jvfault.transport.nats;

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
 * NATS 传输集成测试（真实 broker）。
 *
 * <p>bootstrap 来源优先级：环境变量 {@code JVFAULT_NATS_BOOTSTRAP}（CI service）
 * → 本地 docker 容器（nats:2.10-alpine）→ 不可用则跳过。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
@DisplayName("NATS 传输集成测试（真实 broker）")
class NATSIntegrationTest {

    private static final long TIMEOUT_MS = 20_000L;
    /** broker 可达性探测上限（CI service 冷启动窗口）。 */
    private static final long PROBE_DEADLINE_MS = 30_000L;

    static DockerContainer nats;
    static String bootstrap;
    static boolean brokerUp;

    @BeforeAll
    static void startNats() throws Exception {
        String external = System.getenv("JVFAULT_NATS_BOOTSTRAP");
        if (external != null && !external.trim().isEmpty()) {
            bootstrap = external.trim();
            // 环境变量指向的 broker 可能尚未就绪（CI service 冷启动 / 本机未起）。
            // 不可达时置 brokerUp=false 让测试 skip —— 而不是让整个 class fail。
            if (!isReachable(bootstrap)) {
                brokerUp = false;
                System.err.println("[nats-it] broker 不可达，跳过集成测试: " + bootstrap);
                return;
            }
        } else if (DockerContainer.dockerAvailable() && DockerContainer.hasImage()) {
            nats = new DockerContainer().start();
            bootstrap = nats.getBootstrap();
        } else {
            brokerUp = false;
            return;
        }
        brokerUp = true;
    }

    /**
     * 探测 {@code scheme://host:port} 形式的 bootstrap TCP 可达，带重试。
     * 覆盖 CI service 容器"已映射端口但应用未就绪"的窗口期。
     */
    static boolean isReachable(String bootstrapUrl) {
        String hostPort = bootstrapUrl.contains("://")
                ? bootstrapUrl.substring(bootstrapUrl.indexOf("://") + 3)
                : bootstrapUrl;
        int slash = hostPort.indexOf('/');
        if (slash >= 0) {
            hostPort = hostPort.substring(0, slash);
        }
        int colon = hostPort.lastIndexOf(':');
        String host = colon > 0 ? hostPort.substring(0, colon) : hostPort;
        int port = colon > 0 ? Integer.parseInt(hostPort.substring(colon + 1)) : 80;
        long deadline = System.currentTimeMillis() + PROBE_DEADLINE_MS;
        while (System.currentTimeMillis() < deadline) {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress(host, port), 2_000);
                return true;
            } catch (Exception e) {
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    @AfterAll
    static void stopNats() {
        if (nats != null) {
            nats.close();
        }
    }

    @Test
    @DisplayName("端到端 request-reply（NATS core inbox）")
    void testRequestReply() throws Exception {
        assumeTrue(brokerUp, "需要 NATS broker（Docker 或 JVFAULT_NATS_BOOTSTRAP）");

        JacksonMessageCodec codec = new JacksonMessageCodec();
        NATSTransportServer server = new NATSTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders.create", request -> {
            String sku = codec.decode(request.getData(), String.class);
            return new Message(request.getPattern(), codec.encode("ACK:" + sku),
                    request.getCorrelationId(), null);
        });
        server.bind();

        TransportClient client = new NATSTransportClient(new ConnectionConfig(bootstrap));
        Message response = client.request("orders.create", "SKU-1", TIMEOUT_MS);
        assertEquals("ACK:SKU-1", codec.decode(response.getData(), String.class));

        client.close();
        server.close();
    }

    @Test
    @DisplayName("handler 异常经 error 头回传客户端")
    void testErrorHeader() throws Exception {
        assumeTrue(brokerUp, "需要 NATS broker（Docker 或 JVFAULT_NATS_BOOTSTRAP）");

        NATSTransportServer server = new NATSTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders.invalid", request -> {
            throw new IllegalArgumentException("库存不足");
        });
        server.bind();

        TransportClient client = new NATSTransportClient(new ConnectionConfig(bootstrap));
        TransportException error = assertThrows(TransportException.class,
                () -> client.request("orders.invalid", "SKU-2", TIMEOUT_MS));
        assertTrue(error.getMessage().contains("库存不足"), error.getMessage());

        client.close();
        server.close();
    }

    @Test
    @DisplayName("事件语义：publish 不等待响应")
    void testPublishEvent() throws Exception {
        assumeTrue(brokerUp, "需要 NATS broker（Docker 或 JVFAULT_NATS_BOOTSTRAP）");

        NATSTransportServer server = new NATSTransportServer(new ConnectionConfig(bootstrap));
        server.bind();
        server.publish(new Message("orders.event",
                new JacksonMessageCodec().encode("payload"), null, null));
        assertTrue(server.isRunning());

        NATSTransportClient client = new NATSTransportClient(new ConnectionConfig(bootstrap));
        client.emit("orders.event", "payload");
        client.close();
        server.close();
    }
}
