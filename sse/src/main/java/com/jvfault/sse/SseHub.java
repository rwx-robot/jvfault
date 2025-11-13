package com.jvfault.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 会话注册表 - 按通道广播。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public class SseHub {

    private final Map<String, java.util.List<SseEmitter>> channels = new ConcurrentHashMap<>();

    public void subscribe(String channel, SseEmitter emitter) {
        channels.computeIfAbsent(channel, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> unsubscribe(channel, emitter));
    }

    public void unsubscribe(String channel, SseEmitter emitter) {
        java.util.List<SseEmitter> emitters = channels.get(channel);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    public int subscriberCount(String channel) {
        java.util.List<SseEmitter> emitters = channels.get(channel);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * 向通道广播事件（自动清理已完成/失效的连接）。
     */
    public void broadcast(String channel, String event, String data) {
        java.util.List<SseEmitter> emitters = channels.get(channel);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event, data);
            } catch (IOException e) {
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    public void broadcast(String channel, String data) {
        broadcast(channel, "message", data);
    }
}
