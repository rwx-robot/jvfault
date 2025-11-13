package com.jvfault.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvfault.websocket.annotation.OnConnect;
import com.jvfault.websocket.annotation.SubscribeMessage;
import com.jvfault.websocket.annotation.OnDisconnect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关分发器 - 把 @WebSocketGateway Bean 的 @SubscribeMessage 方法
 * 绑定到事件名，处理连接/断开回调。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public class GatewayDispatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Object gateway;
    private final ConcurrentHashMap<String, Method> eventHandlers = new ConcurrentHashMap<>();

    public GatewayDispatcher(Object gateway) {
        this.gateway = gateway;
        Class<?> clazz = gateway.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                SubscribeMessage sub = method.getAnnotation(SubscribeMessage.class);
                if (sub != null) {
                    method.setAccessible(true);
                    eventHandlers.put(sub.value(), method);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    public Object getGateway() {
        return gateway;
    }

    public boolean handles(String event) {
        return eventHandlers.containsKey(event);
    }

    /**
     * 连接回调：调用 @OnConnect 方法（若有）。
     */
    public void onConnect(com.jvfault.websocket.net.WebSocketSession session) {
        invokeLifecycle(OnConnect.class, session);
    }

    /**
     * 断开回调：调用 @OnDisconnect 方法（若有）。
     */
    public void onDisconnect(com.jvfault.websocket.net.WebSocketSession session) {
        invokeLifecycle(OnDisconnect.class, session);
    }

    /**
     * 事件分发：payload JSON 反序列化为方法首参类型。
     */
    public void dispatch(com.jvfault.websocket.net.WebSocketSession session, String event, String payload) {
        Method handler = eventHandlers.get(event);
        if (handler == null) {
            throw new IllegalArgumentException("网关未处理事件: " + event);
        }
        try {
            Class<?> payloadType = handler.getParameterCount() > 0 ? handler.getParameterTypes()[0] : String.class;
            Object arg = "String".equals(payloadType.getSimpleName()) ? payload
                    : MAPPER.readValue(payload == null ? "{}" : payload, payloadType);
            handler.setAccessible(true);
            if (handler.getParameterCount() > 1
                    && handler.getParameterTypes()[1] == com.jvfault.websocket.net.WebSocketSession.class) {
                handler.invoke(gateway, arg, session);
            } else {
                handler.invoke(gateway, arg);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("事件处理失败: " + event, cause);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("事件分发失败: " + event, e);
        }
    }

    private void invokeLifecycle(Class<? extends java.lang.annotation.Annotation> type,
                                 com.jvfault.websocket.net.WebSocketSession session) {
        Class<?> clazz = gateway.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(type)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(gateway, session);
                    } catch (Exception e) {
                        throw new IllegalStateException("生命周期回调失败", e);
                    }
                    return;
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
