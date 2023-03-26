package com.jvfault.sse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * SSE 事件发送器 - 把事件流写到输出流。
 *
 * <p>输出流由平台适配器提供（Servlet 异步上下文等）。
 *
 * @since v0.5.0 (2019)
 * @author jvfault team
 */
public class SseEmitter {

    private final OutputStream output;
    private volatile boolean completed;
    private volatile Runnable onCompletion;

    public SseEmitter(OutputStream output) {
        this.output = output;
        // 发送响应头
        write("Content-Type: text/event-stream\r\nCache-Control: no-cache\r\n\r\n");
    }

    /**
     * 发送事件。
     */
    public synchronized void send(String event, String data) throws IOException {
        send(event, data, null);
    }

    public synchronized void send(String event, String data, String id) throws IOException {
        if (completed) {
            throw new IOException("SSE 已完成");
        }
        StringBuilder sb = new StringBuilder();
        if (id != null) {
            sb.append("id: ").append(id).append('\n');
        }
        sb.append("event: ").append(event).append('\n');
        for (String line : data.split("\n", -1)) {
            sb.append("data: ").append(line).append('\n');
        }
        sb.append('\n');
        write(sb.toString());
    }

    /**
     * 心跳注释（保持连接）。
     */
    public synchronized void heartbeat() throws IOException {
        write(": ping\n\n");
    }

    public synchronized void complete() {
        if (!completed) {
            completed = true;
            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void onCompletion(Runnable callback) {
        this.onCompletion = callback;
    }

    private void write(String text) {
        try {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) {
            completed = true;
        }
    }
}
