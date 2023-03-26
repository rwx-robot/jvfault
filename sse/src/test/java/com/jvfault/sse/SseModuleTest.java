package com.jvfault.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-sse 核心测试
 *
 * @since v0.5.0 (2019)
 */
@DisplayName("SSE 模块测试")
class SseModuleTest {

    @Test
    @DisplayName("SseEmitter 输出标准事件流格式")
    void testEmitterFormat() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SseEmitter emitter = new SseEmitter(out);

        try {
            emitter.send("stock", "AAPL=142.5", "42");
        } catch (IOException e) {
            fail(e);
        }

        String output = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Content-Type: text/event-stream"));
        assertTrue(output.contains("id: 42"));
        assertTrue(output.contains("event: stock"));
        assertTrue(output.contains("data: AAPL=142.5"));
        assertTrue(output.endsWith("\n\n"));
    }

    @Test
    @DisplayName("多行 data 逐行输出")
    void testMultiLineData() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SseEmitter emitter = new SseEmitter(out);
        emitter.send("msg", "line1\nline2");

        String output = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.contains("data: line1\ndata: line2\n"));
    }

    @Test
    @DisplayName("complete 后发送抛异常且回调触发")
    void testCompletion() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SseEmitter emitter = new SseEmitter(out);
        boolean[] called = {false};
        emitter.onCompletion(() -> called[0] = true);

        assertFalse(emitter.isCompleted());
        emitter.complete();
        assertTrue(emitter.isCompleted());
        assertTrue(called[0], "完成回调应触发");
        assertThrows(IOException.class, () -> emitter.send("e", "d"));
    }

    @Test
    @DisplayName("Hub 通道订阅与广播")
    void testHubBroadcast() throws IOException {
        SseHub hub = new SseHub();
        ByteArrayOutputStream outA = new ByteArrayOutputStream();
        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        ByteArrayOutputStream outC = new ByteArrayOutputStream();

        SseEmitter a = new SseEmitter(outA);
        SseEmitter b = new SseEmitter(outB);
        SseEmitter c = new SseEmitter(outC);
        hub.subscribe("ticks", a);
        hub.subscribe("ticks", b);
        hub.subscribe("news", c);

        assertEquals(2, hub.subscriberCount("ticks"));
        assertEquals(1, hub.subscriberCount("news"));

        hub.broadcast("ticks", "price", "AAPL=142.5");

        assertTrue(new String(outA.toByteArray(), StandardCharsets.UTF_8).contains("AAPL=142.5"));
        assertTrue(new String(outB.toByteArray(), StandardCharsets.UTF_8).contains("AAPL=142.5"));
        assertFalse(new String(outC.toByteArray(), StandardCharsets.UTF_8).contains("AAPL=142.5"));
    }

    @Test
    @DisplayName("完成连接自动从 Hub 摘除")
    void testAutoUnsubscribeOnComplete() throws IOException {
        SseHub hub = new SseHub();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SseEmitter emitter = new SseEmitter(out);
        hub.subscribe("ch", emitter);
        assertEquals(1, hub.subscriberCount("ch"));

        emitter.complete();
        assertEquals(0, hub.subscriberCount("ch"), "完成回调应触发摘除");
    }

    @Test
    @DisplayName("广播到断开连接被清理")
    void testBroadcastToBrokenConnection() {
        SseHub hub = new SseHub();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SseEmitter broken = new SseEmitter(out) {
            @Override
            public synchronized void send(String event, String data, String id) throws IOException {
                throw new IOException("broken pipe");
            }
        };
        hub.subscribe("ch", broken);
        hub.subscribe("ch", new SseEmitter(new ByteArrayOutputStream()));

        hub.broadcast("ch", "data");
        assertEquals(1, hub.subscriberCount("ch"), "失效连接应被清理");
    }
}
