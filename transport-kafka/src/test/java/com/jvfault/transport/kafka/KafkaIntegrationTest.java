package com.jvfault.transport.kafka;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.TransportException;
import org.apache.kafka.clients.admin.Admin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kafka 传输集成测试（真实 broker）。
 *
 * <p>bootstrap 来源优先级：环境变量 {@code JVFAULT_KAFKA_BOOTSTRAP}（CI service）
 * → 本地 docker 容器（apache/kafka:3.7.0, KRaft 单节点）→ 不可用则跳过。
 *
 * @since v1.0.1 (2026)
 * @author jvfault team
 */
@DisplayName("Kafka 传输集成测试（真实 broker）")
class KafkaIntegrationTest {

    private static final String IMAGE = "apache/kafka:3.7.0";
    private static final long TIMEOUT_MS = 20_000L;

    static DockerContainer kafka;
    static String bootstrap;
    static boolean brokerUp;

    @BeforeAll
    static void startKafka() throws Exception {
        String external = System.getenv("JVFAULT_KAFKA_BOOTSTRAP");
        if (external != null && !external.trim().isEmpty()) {
            bootstrap = external.trim();
        } else if (DockerContainer.dockerAvailable() && hasImage(IMAGE)) {
            kafka = new DockerContainer(IMAGE, "9092/tcp").start(kraftEnv());
            bootstrap = "localhost:9092";
        } else {
            brokerUp = false;
            return;
        }
        // broker 未就绪（CI service 冷启动 / 本机未起）时降级为 skip，
        // 不要让 @BeforeAll 抛错把整个 class 判失败。
        try {
            waitForBroker(bootstrap);
        } catch (Exception e) {
            brokerUp = false;
            System.err.println("[kafka-it] broker 未就绪，跳过集成测试: "
                    + bootstrap + " (" + e.getMessage() + ")");
            return;
        }
        brokerUp = true;
    }

    @AfterAll
    static void stopKafka() {
        if (kafka != null) {
            kafka.close();
        }
    }

    private static String[] kraftEnv() {
        return new String[]{
                "KAFKA_NODE_ID=1",
                "KAFKA_PROCESS_ROLES=broker,controller",
                "KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093",
                "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092",
                "KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093",
                "KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER",
                "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT",
                "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1",
                "KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1",
                "KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1",
                "KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0"
        };
    }

    private static boolean hasImage(String image) {
        try {
            DockerContainer.ensureImage(image);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 轮询 describeCluster 直到 broker 就绪（容器冷启动可能需数十秒）。 */
    private static void waitForBroker(String bootstrapServers) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", "jvfault-it-probe");
        props.put("request.timeout.ms", "5000");
        props.put("default.api.timeout.ms", "5000");
        long deadline = System.currentTimeMillis() + 90_000L;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Admin admin = Admin.create(props)) {
                admin.describeCluster().nodes().get(10, TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                last = e;
                Thread.sleep(1_000L);
            }
        }
        throw new IllegalStateException("Kafka broker 未就绪: " + bootstrapServers, last);
    }

    @Test
    @DisplayName("端到端 request-reply（topic 即 pattern，replyTopic 回程）")
    void testRequestReply() throws Exception {
        assumeTrue(brokerUp, "需要 Kafka broker（Docker 或 JVFAULT_KAFKA_BOOTSTRAP）");

        JacksonMessageCodec codec = new JacksonMessageCodec();
        KafkaTransportServer server = new KafkaTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders.create", request -> {
            String sku = codec.decode(request.getData(), String.class);
            return new Message(request.getPattern(), codec.encode("ACK:" + sku),
                    request.getCorrelationId(), null);
        });
        server.bind();

        TransportClient client = new KafkaTransportClient(new ConnectionConfig(bootstrap));
        Message response = client.request("orders.create", "SKU-1", TIMEOUT_MS);
        assertEquals("ACK:SKU-1", codec.decode(response.getData(), String.class));

        client.close();
        server.close();
    }

    @Test
    @DisplayName("handler 异常经 error 头回传客户端")
    void testErrorHeader() throws Exception {
        assumeTrue(brokerUp, "需要 Kafka broker（Docker 或 JVFAULT_KAFKA_BOOTSTRAP）");

        KafkaTransportServer server = new KafkaTransportServer(new ConnectionConfig(bootstrap));
        server.subscribe("orders.invalid", request -> {
            throw new IllegalArgumentException("库存不足");
        });
        server.bind();

        TransportClient client = new KafkaTransportClient(new ConnectionConfig(bootstrap));
        TransportException error = assertThrows(TransportException.class,
                () -> client.request("orders.invalid", "SKU-2", TIMEOUT_MS));
        assertTrue(error.getMessage().contains("库存不足"), error.getMessage());

        client.close();
        server.close();
    }

    @Test
    @DisplayName("事件语义：publish 不等待响应")
    void testPublishEvent() throws Exception {
        assumeTrue(brokerUp, "需要 Kafka broker（Docker 或 JVFAULT_KAFKA_BOOTSTRAP）");

        KafkaTransportServer server = new KafkaTransportServer(new ConnectionConfig(bootstrap));
        server.bind();
        server.publish(new Message("orders.event",
                new JacksonMessageCodec().encode("payload"), null, null));
        assertTrue(server.isRunning());

        KafkaTransportClient client = new KafkaTransportClient(new ConnectionConfig(bootstrap));
        client.emit("orders.event", "payload");
        client.close();
        server.close();
    }
}
