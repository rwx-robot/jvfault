package com.jvfault.microservices;

import com.jvfault.microservices.annotation.EventPattern;
import com.jvfault.microservices.annotation.MessageHandler;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-microservices 核心测试
 *
 * @since v0.6.0 (2020)
 */
@DisplayName("Microservices 模块测试")
class MicroservicesModuleTest {

    // ============ 模式匹配 ============

    @Test
    @DisplayName("PatternMatcher: 精确/*/＃")
    void testPatternMatcher() {
        assertTrue(PatternMatcher.matches("user.created", "user.created"));
        assertFalse(PatternMatcher.matches("user.created", "user.deleted"));

        assertTrue(PatternMatcher.matches("user.*.created", "user.42.created"));
        assertFalse(PatternMatcher.matches("user.*.created", "user.created"));
        assertFalse(PatternMatcher.matches("user.*.created", "user.a.b.created"));

        assertTrue(PatternMatcher.matches("log.#", "log.a.b.c"));
        assertTrue(PatternMatcher.matches("log.#", "log"));
        assertFalse(PatternMatcher.matches("log.#", "audit.log"));
    }

    // ============ 编解码 ============

    @Test
    @DisplayName("JacksonMessageCodec 往返")
    void testCodec() {
        MessageCodec codec = new JacksonMessageCodec();
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("name", "jvfault");
        payload.put("count", 3);
        byte[] encoded = codec.encode(payload);
        Map decoded = codec.decode(encoded, Map.class);
        assertEquals("jvfault", decoded.get("name"));
        assertEquals(3, decoded.get("count"));
    }

    // ============ InMemory 传输 ============

    @Test
    @DisplayName("InMemory request-reply 往返")
    void testRequestReply() {
        InMemoryTransport server = new InMemoryTransport();
        TransportClient client = server.createClient();
        server.subscribe("math.sum", req -> {
            MessageCodec codec = new JacksonMessageCodec();
            int[] nums = codec.decode(req.getData(), int[].class);
            int sum = nums[0] + nums[1];
            return new Message(req.getPattern(), codec.encode(sum), req.getId(), null);
        });

        Message response = client.request("math.sum", new int[]{19, 23}, 2000);
        assertEquals(42, new JacksonMessageCodec().decode(response.getData(), Integer.class));
        server.close();
    }

    @Test
    @DisplayName("emit 事件到达订阅者")
    void testEmit() {
        InMemoryTransport server = new InMemoryTransport();
        TransportClient client = server.createClient();
        ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        server.subscribe("user.created", req -> {
            received.add(new String(req.getData()));
            return null;
        });

        client.emit("user.created", "alice");
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> !received.isEmpty());
        assertTrue(new String(received.peek()).contains("alice"));
        server.close();
    }

    @Test
    @DisplayName("request 无处理器 -> 超时异常")
    void testRequestTimeout() {
        InMemoryTransport server = new InMemoryTransport();
        TransportClient client = server.createClient();
        assertThrows(TransportTimeoutException.class,
                () -> client.request("no.handler", "x", 100));
        server.close();
    }

    // ============ 注解处理器 ============

    static class GreetingHandlers {
        @MessageHandler("greet.hello")
        public String hello(String name) {
            return "Hello, " + name + "!";
        }

        @EventPattern("audit.logged")
        public void onAudit(String event) {
            events.add(event);
        }

        @MessageHandler("fail.always")
        public String fail(String input) {
            throw new IllegalStateException("boom");
        }

        final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();
    }

    @Test
    @DisplayName("注解 handler 注册与返回值回发")
    void testAnnotationHandlers() {
        InMemoryTransport server = new InMemoryTransport();
        GreetingHandlers handlers = new GreetingHandlers();

        MessageHandlerAnnotationPostProcessor bpp =
                new MessageHandlerAnnotationPostProcessor(server);
        bpp.postProcessAfterInitialization(handlers, "greetingHandlers");

        TransportClient client = server.createClient();
        Message response = client.request("greet.hello", "jvfault", 2000);
        assertEquals("Hello, jvfault!", new JacksonMessageCodec().decode(response.getData(), String.class));
        server.close();
    }

    @Test
    @DisplayName("事件注解方法接收 payload")
    void testEventAnnotation() {
        InMemoryTransport server = new InMemoryTransport();
        GreetingHandlers handlers = new GreetingHandlers();
        new MessageHandlerAnnotationPostProcessor(server).postProcessAfterInitialization(handlers, "h");

        server.createClient().emit("audit.logged", "login");
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> !handlers.events.isEmpty());
        assertTrue(handlers.events.peek().contains("login"));
        server.close();
    }

    @Test
    @DisplayName("handler 异常转 error 响应")
    void testHandlerError() {
        InMemoryTransport server = new InMemoryTransport();
        GreetingHandlers handlers = new GreetingHandlers();
        new MessageHandlerAnnotationPostProcessor(server).postProcessAfterInitialization(handlers, "h");

        Message raw = new Message("fail.always", new JacksonMessageCodec().encode("x"));
        Message response = server.request(raw, 1000);
        assertNotNull(response.getHeader(Message.HEADER_ERROR));
        assertTrue(response.getHeader(Message.HEADER_ERROR).contains("boom"));
        server.close();
    }

    // ============ 客户端代理 ============

    interface MathApi {
        @RequestPattern("math.double")
        int doubleIt(int value);

        void fire(String event);
    }

    @Test
    @DisplayName("客户端接口代理（request + emit）")
    void testClientProxy() {
        InMemoryTransport server = new InMemoryTransport();
        server.subscribe("math.double", req -> {
            MessageCodec codec = new JacksonMessageCodec();
            int v = codec.decode(req.getData(), Integer.class);
            return new Message(req.getPattern(), codec.encode(v * 2), req.getId(), null);
        });

        RequestReplyClientFactory factory = new RequestReplyClientFactory(server.createClient(), 2000);
        MathApi api = factory.create(MathApi.class);

        assertEquals(42, api.doubleIt(21));

        ConcurrentLinkedQueue<String> fired = new ConcurrentLinkedQueue<>();
        server.subscribe("fire", req -> {
            fired.add(new String(req.getData()));
            return null;
        });
        api.fire("go");
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> !fired.isEmpty());
        server.close();
    }
}
