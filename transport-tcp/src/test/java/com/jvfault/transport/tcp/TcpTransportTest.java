package com.jvfault.transport.tcp;

import com.jvfault.microservices.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-transport-tcp 核心测试
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("Transport-TCP 测试")
class TcpTransportTest {

    @Test
    @DisplayName("帧封装与解包往返")
    void testFraming() {
        byte[] payload = "hello-framing".getBytes();
        byte[] framed = TcpMessageFraming.frame(payload);

        byte[] unframed = TcpMessageFraming.tryUnframe(new ByteArrayInputStream(framed));
        assertArrayEquals(payload, unframed);
    }

    @Test
    @DisplayName("不完整帧返回 null")
    void testPartialFrame() {
        byte[] payload = "abcde".getBytes();
        byte[] framed = TcpMessageFraming.frame(payload);
        byte[] truncated = java.util.Arrays.copyOf(framed, framed.length - 2);

        assertNull(TcpMessageFraming.tryUnframe(new ByteArrayInputStream(truncated)));
    }

    @Test
    @DisplayName("TCP 端到端 request-reply")
    void testEndToEnd() throws Exception {
        int port = 27_510 + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);
        TcpTransportServer server = new TcpTransportServer(port);
        server.subscribe("echo.upper", req -> {
            com.jvfault.microservices.MessageCodec codec = new com.jvfault.microservices.JacksonMessageCodec();
            String text = codec.decode(req.getData(), String.class);
            return new Message(req.getPattern(), codec.encode(text.toUpperCase()), req.getId(), null);
        });
        server.bind();

        TcpTransportClient client = new TcpTransportClient("127.0.0.1", port);
        try {
            Message response = client.request("echo.upper", "ping", 5000);
            assertEquals("PING", new com.jvfault.microservices.JacksonMessageCodec()
                    .decode(response.getData(), String.class));
        } finally {
            client.close();
            server.close();
        }
    }

    @Test
    @DisplayName("连接拒绝抛 TransportException")
    void testConnectionRefused() {
        TcpTransportClient client = new TcpTransportClient("127.0.0.1", 1);
        assertThrows(com.jvfault.microservices.TransportException.class,
                () -> client.request("any", "x", 500));
    }
}
