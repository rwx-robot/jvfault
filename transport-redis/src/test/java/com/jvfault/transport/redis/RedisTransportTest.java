package com.jvfault.transport.redis;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 传输集成测试（docker CLI 管理真实 Redis 容器；
 * Docker 不可用时跳过）。
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("Redis 传输集成测试")
class RedisTransportTest {

    static DockerContainer redis;
    static String bootstrap;
    static boolean brokerUp;

    @BeforeAll
    static void startRedis() throws Exception {
        // CI/外部 broker 优先：JVFAULT_REDIS_BOOTSTRAP=redis://127.0.0.1:6379
        String external = System.getenv("JVFAULT_REDIS_BOOTSTRAP");
        if (external != null && !external.trim().isEmpty()) {
            bootstrap = external.trim();
            // 环境变量指向的 broker 可能尚未就绪（CI service 冷启动 / 本机未起）。
            // 不可达时置 brokerUp=false 让测试 skip —— 而不是让整个 class fail。
            if (!isReachable(bootstrap)) {
                brokerUp = false;
                System.err.println("[redis-it] broker 不可达，跳过集成测试: " + bootstrap);
                return;
            }
            brokerUp = true;
            return;
        }
        if (!DockerContainer.dockerAvailable() || !DockerContainer.hasImage("redis:7.2-alpine")) {
            brokerUp = false;
            return;
        }
        redis = new DockerContainer("redis:7.2-alpine", "6379/tcp").start();
        String portMapping = redis.hostPort(); // 形如 0.0.0.0:32768
        String port = portMapping.substring(portMapping.lastIndexOf(':') + 1);
        bootstrap = "redis://127.0.0.1:" + port;
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
        long deadline = System.currentTimeMillis() + 30_000L;
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
    static void stopRedis() {
        if (redis != null) {
            redis.close();
        }
    }

    @Test
    @DisplayName("端到端 request-reply（psubscribe + replyChannel）")
    void testRequestReply() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(brokerUp, "需要 Redis broker（Docker 或 JVFAULT_REDIS_BOOTSTRAP）");

        RedisTransportServer server = new RedisTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("user.get.*", req -> {
            JacksonMessageCodec codec = new JacksonMessageCodec();
            String name = codec.decode(req.getData(), String.class);
            return new Message(req.getPattern(), codec.encode("Hello, " + name), req.getCorrelationId(), null);
        });
        server.bind();

        TransportClient client = new RedisTransportClient(new ConnectionConfig(bootstrap));
        Message response = client.request("user.get.42", "alice", 5000);
        assertEquals("Hello, alice",
                new JacksonMessageCodec().decode(response.getData(), String.class));

        client.close();
        server.close();
    }

    @Test
    @DisplayName("handler 异常经 error 头回传")
    void testErrorPropagation() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(brokerUp, "需要 Redis broker（Docker 或 JVFAULT_REDIS_BOOTSTRAP）");

        RedisTransportServer server = new RedisTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("fail.always", req -> {
            throw new IllegalStateException("boom");
        });
        server.bind();

        TransportClient client = new RedisTransportClient(new ConnectionConfig(bootstrap));
        TransportException ex = assertThrows(TransportException.class,
                () -> client.request("fail.always", "x", 5000));
        assertTrue(ex.getMessage().contains("boom"));
        client.close();
        server.close();
    }

    @Test
    @DisplayName("空 bootstrap 校验失败")
    void testConfigValidation() {
        assertThrows(com.jvfault.microservices.TransportException.class,
                () -> new ConnectionConfig("  ").validate());
    }
}
