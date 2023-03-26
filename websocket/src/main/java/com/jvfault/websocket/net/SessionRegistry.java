package com.jvfault.websocket.net;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话注册表 - 房间（room）与广播。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public class SessionRegistry {

    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session.getId());
        for (CopyOnWriteArrayList<WebSocketSession> members : rooms.values()) {
            members.remove(session);
        }
    }

    public WebSocketSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public int sessionCount() {
        return sessions.size();
    }

    public void join(String room, WebSocketSession session) {
        rooms.computeIfAbsent(room, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void leave(String room, WebSocketSession session) {
        CopyOnWriteArrayList<WebSocketSession> members = rooms.get(room);
        if (members != null) {
            members.remove(session);
        }
    }

    public int roomSize(String room) {
        CopyOnWriteArrayList<WebSocketSession> members = rooms.get(room);
        return members != null ? members.size() : 0;
    }

    /**
     * 向会话发送（忽略断开的会话）。
     */
    public void sendTo(String sessionId, String text) throws IOException {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendText(text);
        }
    }

    /**
     * 房间广播。
     */
    public void broadcast(String room, String text) {
        CopyOnWriteArrayList<WebSocketSession> members = rooms.get(room);
        if (members == null) {
            return;
        }
        for (WebSocketSession session : members) {
            if (session.isOpen()) {
                try {
                    session.sendText(text);
                } catch (IOException e) {
                    // 发送失败即视为断开
                    unregister(session);
                }
            }
        }
    }

    /**
     * 全体广播。
     */
    public void broadcastAll(String text) {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendText(text);
                } catch (IOException e) {
                    unregister(session);
                }
            }
        }
    }
}
