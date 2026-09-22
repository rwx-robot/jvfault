package com.jvfault.websocket.net;

import java.io.IOException;

/**
 * WebSocket 会话抽象（由具体服务器适配器实现）。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public interface WebSocketSession {

    String getId();

    boolean isOpen();

    void sendText(String text) throws IOException;

    void close() throws IOException;
}
