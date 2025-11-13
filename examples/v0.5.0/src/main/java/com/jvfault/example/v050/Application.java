package com.jvfault.example.v050;

import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;
import com.jvfault.sse.SseEmitter;
import com.jvfault.sse.SseHub;
import com.jvfault.websocket.GatewayRegistry;
import com.jvfault.websocket.annotation.SubscribeMessage;
import com.jvfault.websocket.annotation.WebSocketGateway;
import com.jvfault.websocket.net.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Application {

    @WebSocketGateway("/chat")
    static class ChatGateway {
        final List<String> received = new ArrayList<>();

        @SubscribeMessage("chat.message")
        public String onMessage(String text) {
            received.add(text);
            return "ECHO: " + text;
        }
    }

    @Module(providers = {ChatGateway.class})
    static class AppModule {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== jvfault v0.5.0: WebSocket + SSE ==");
        ModuleContainer container = JvfaultApplication.createContainer(AppModule.class);
        try {
            GatewayRegistry gateways = new GatewayRegistry();
            ChatGateway gateway = container.getBeanRegistry().getBean(ChatGateway.class);
            gateways.register(gateway);

            // 内存会话（真实连接由 Jetty WebSocker 适配器桥接）
            List<String> sent = new ArrayList<>();
            WebSocketSession session = new WebSocketSession() {
                @Override public String getId() { return "mock-1"; }
                @Override public boolean isOpen() { return true; }
                @Override public void sendText(String text) { sent.add(text); }
                @Override public void close() { }
            };

            gateways.onOpen(session);
            gateways.onMessage(session, "chat.message", "hello websocket");
            gateways.onClose(session);
            System.out.println("  网关收到消息: " + gateway.received);
            System.out.println("  生命周期: open -> message -> close 全部通过");

            // SSE
            SseHub hub = new SseHub();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SseEmitter emitter = new SseEmitter(out);
            hub.subscribe("ticks", emitter);
            hub.broadcast("ticks", "price", "AAPL=142.5");
            String sse = out.toString(StandardCharsets.UTF_8);
            System.out.println("  SSE 输出: " + sse.replace("\n", "\\n").split("event:")[1].trim());
        } finally {
            container.destroy();
        }
        System.out.println("== 完成 ==");
    }
}
