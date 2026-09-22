package com.jvfault.websocket;

import com.jvfault.websocket.net.SessionRegistry;
import com.jvfault.websocket.annotation.OnConnect;
import com.jvfault.websocket.annotation.OnDisconnect;
import com.jvfault.websocket.annotation.SubscribeMessage;
import com.jvfault.websocket.annotation.WebSocketGateway;
import com.jvfault.websocket.net.WebSocketSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-websocket 核心测试
 *
 * @since v0.5.0 (2019)
 */
@DisplayName("WebSocket 模块测试")
class WebsocketModuleTest {

    static class MockSession implements WebSocketSession {
        private final String id;
        private final List<String> sent = new ArrayList<>();
        private boolean open = true;

        MockSession(String id) {
            this.id = id;
        }

        @Override public String getId() { return id; }
        @Override public boolean isOpen() { return open; }
        @Override public void sendText(String text) {
            if (!open) throw new IllegalStateException("closed");
            sent.add(text);
        }
        @Override public void close() { open = false; }

        List<String> getSent() { return sent; }
    }

    @WebSocketGateway("/chat")
    static class ChatGateway {
        final List<String> connected = new ArrayList<>();
        final List<String> disconnected = new ArrayList<>();
        final List<String> messages = new ArrayList<>();

        @OnConnect
        public void onConnect(WebSocketSession session) {
            connected.add(session.getId());
        }

        @OnDisconnect
        public void onDisconnect(WebSocketSession session) {
            disconnected.add(session.getId());
        }

        @SubscribeMessage("chat.message")
        public ChatMessage onMessage(ChatMessage message, WebSocketSession session) {
            messages.add(session.getId() + ":" + message.text);
            message.text = "echo: " + message.text;
            return message;
        }

        @SubscribeMessage("join")
        public void join(WebSocketSession session) {
            // 无 payload 处理器
        }
    }

    static class ChatMessage {
        public String text;
        public String room;
    }

    @Test
    @DisplayName("网关注册与连接生命周期")
    void testLifecycle() {
        GatewayRegistry registry = new GatewayRegistry();
        ChatGateway gateway = new ChatGateway();
        registry.register(gateway);
        assertEquals(1, registry.gatewayCount());

        MockSession session = new MockSession("s1");
        registry.onOpen(session);
        assertEquals(java.util.Collections.singletonList("s1"), gateway.connected);
        assertEquals(1, registry.getSessions().sessionCount());

        registry.onClose(session);
        assertEquals(java.util.Collections.singletonList("s1"), gateway.disconnected);
        assertEquals(0, registry.getSessions().sessionCount());
    }

    @Test
    @DisplayName("事件分发与 JSON payload 绑定")
    void testEventDispatch() {
        GatewayRegistry registry = new GatewayRegistry();
        ChatGateway gateway = new ChatGateway();
        registry.register(gateway);

        MockSession session = new MockSession("s1");
        registry.onOpen(session);

        registry.onMessage(session, "chat.message", "{\"text\":\"hello\",\"room\":\"general\"}");
        assertEquals("s1:hello", gateway.messages.get(0));

        assertThrows(IllegalArgumentException.class,
                () -> registry.onMessage(session, "unknown.event", "{}"));
    }

    @Test
    @DisplayName("房间广播与按会话发送")
    void testRoomsAndBroadcast() throws IOException {
        SessionRegistry sessions = new SessionRegistry();
        MockSession a = new MockSession("a");
        MockSession b = new MockSession("b");
        MockSession c = new MockSession("c");

        sessions.register(a);
        sessions.register(b);
        sessions.register(c);
        sessions.join("room1", a);
        sessions.join("room1", b);
        sessions.join("room2", c);

        assertEquals(2, sessions.roomSize("room1"));
        sessions.broadcast("room1", "hello-room");
        assertEquals(1, a.getSent().size());
        assertEquals(1, b.getSent().size());
        assertEquals(0, c.getSent().size());

        sessions.sendTo("c", "direct");
        assertEquals(1, c.getSent().size());

        sessions.broadcastAll("all");
        assertEquals(2, a.getSent().size());

        sessions.unregister(b);
        sessions.broadcast("room1", "again");
        assertEquals(3, a.getSent().size());
        assertEquals(2, b.getSent().size(), "已注销会话不再新增（此前已收 2 条）");
    }

    @Test
    @DisplayName("断开会话发送失败被容错处理")
    void testSendToClosedSessionTolerated() {
        SessionRegistry sessions = new SessionRegistry();
        MockSession closed = new MockSession("x");
        closed.close();
        sessions.register(closed);
        sessions.join("room", closed);
        assertDoesNotThrow(() -> sessions.broadcast("room", "to-closed"));
    }
}
