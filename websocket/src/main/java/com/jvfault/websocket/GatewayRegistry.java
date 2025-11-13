package com.jvfault.websocket;

import com.jvfault.websocket.net.SessionRegistry;
import com.jvfault.websocket.net.WebSocketSession;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 网关注册表 - 收集容器中的 @WebSocketGateway Bean 并提供统一入口。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public class GatewayRegistry {

    private final List<GatewayDispatcher> gateways = new CopyOnWriteArrayList<>();
    private final SessionRegistry sessions = new SessionRegistry();

    /**
     * 注册网关 Bean（非 @WebSocketGateway 类忽略）。
     */
    public void register(Object candidate) {
        if (candidate != null && candidate.getClass()
                .isAnnotationPresent(com.jvfault.websocket.annotation.WebSocketGateway.class)) {
            gateways.add(new GatewayDispatcher(candidate));
        }
    }

    public int gatewayCount() {
        return gateways.size();
    }

    public SessionRegistry getSessions() {
        return sessions;
    }

    /**
     * 连接建立：注册会话并触发网关 onConnect。
     */
    public void onOpen(WebSocketSession session) {
        sessions.register(session);
        for (GatewayDispatcher gateway : gateways) {
            gateway.onConnect(session);
        }
    }

    /**
     * 事件消息分发（首个处理该事件的网关响应）。
     */
    public void onMessage(WebSocketSession session, String event, String payload) {
        for (GatewayDispatcher gateway : gateways) {
            if (gateway.handles(event)) {
                gateway.dispatch(session, event, payload);
                return;
            }
        }
        throw new IllegalArgumentException("无网关处理事件: " + event);
    }

    /**
     * 连接关闭：触发 onDisconnect 并注销会话。
     */
    public void onClose(WebSocketSession session) {
        for (GatewayDispatcher gateway : gateways) {
            gateway.onDisconnect(session);
        }
        sessions.unregister(session);
    }
}
