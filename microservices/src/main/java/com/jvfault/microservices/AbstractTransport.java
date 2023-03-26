package com.jvfault.microservices;

import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 传输基类 - 提供订阅表与模式匹配分发。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public abstract class AbstractTransport implements TransportServer {

    protected final ConcurrentHashMap<String, List<MessageHandler>> handlers = new ConcurrentHashMap<>();
    protected volatile boolean running;

    @Override
    public void subscribe(String pattern, MessageHandler handler) {
        handlers.computeIfAbsent(pattern, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public void bind() {
        running = true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 把消息分发给所有匹配的处理器，返回首个非空响应。
     */
    protected Message dispatch(Message message) {
        List<MessageHandler> matched = new ArrayList<>();
        for (Map.Entry<String, List<MessageHandler>> e : handlers.entrySet()) {
            if (PatternMatcher.matches(e.getKey(), message.getPattern())) {
                matched.addAll(e.getValue());
            }
        }
        for (MessageHandler handler : matched) {
            Message response = handler.handle(message);
            if (response != null) {
                return response;
            }
        }
        return null;
    }

}
