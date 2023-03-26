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
    static boolean dockerUp;

    @BeforeAll
    static void startRedis() throws Exception {
        dockerUp = DockerContainer.dockerAvailable();
        if (!dockerUp) {
            return;
        }
        DockerContainer.ensureImage("redis:7.2-alpine");
        redis = new DockerContainer("redis:7.2-alpine", "6379/tcp").start();
        String portMapping = redis.hostPort(); // 形如 0.0.0.0:32768
        String port = portMapping.substring(portMapping.lastIndexOf(':') + 1);
        bootstrap = "redis://127.0.0.1:" + port;
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
        org.junit.jupiter.api.Assumptions.assumeTrue(dockerUp, "需要 Docker");

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
        org.junit.jupiter.api.Assumptions.assumeTrue(dockerUp, "需要 Docker");

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
