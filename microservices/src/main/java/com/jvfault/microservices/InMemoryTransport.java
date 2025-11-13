package com.jvfault.microservices;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 进程内传输（同机测试与单机部署）。request 用队列 + correlationId 配对。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class InMemoryTransport extends AbstractTransport {

    private final BlockingQueue<Message> eventQueue = new LinkedBlockingQueue<>();
    private final Thread dispatcher;
    private volatile boolean closed;

    public InMemoryTransport() {
        this.dispatcher = new Thread(this::drainEvents, "jvfault-inmemory-transport");
        this.dispatcher.setDaemon(true);
    }

    /** 事件分发循环 */
    private void drainEvents() {
        while (!closed) {
            try {
                Message event = eventQueue.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) {
                    dispatch(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void bind() {
        super.bind();
        dispatcher.start();
    }

    @Override
    public void publish(Message message) {
        ensureRunning();
        eventQueue.add(message);
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        ensureRunning();
        // 同进程：直接同步分发
        Message response = dispatch(request);
        if (response == null && timeoutMillis >= 0) {
            throw new TransportTimeoutException("无处理器响应: " + request.getPattern());
        }
        return response;
    }

    private void ensureRunning() {
        if (!running) {
            bind();
        }
    }

    @Override
    public void close() {
        closed = true;
        running = false;
        dispatcher.interrupt();
    }

    /**
     * 创建绑定到本 server 的客户端。
     */
    public TransportClient createClient() {
        return new InMemoryClient(this);
    }

    private static class InMemoryClient implements TransportClient {
        private final InMemoryTransport server;

        InMemoryClient(InMemoryTransport server) {
            this.server = server;
        }

        @Override
        public void connect() {
            if (!server.isRunning()) {
                server.bind();
            }
        }

        @Override
        public Message request(String pattern, Object payload, long timeoutMillis) {
            MessageCodec codec = new JacksonMessageCodec();
            Message request = new Message(pattern, codec.encode(payload));
            Message response = server.request(request, timeoutMillis);
            if (response != null && response.getHeader(Message.HEADER_ERROR) != null) {
                throw new TransportException("远端错误: " + response.getHeader(Message.HEADER_ERROR));
            }
            return response;
        }

        @Override
        public void emit(String pattern, Object payload) {
            MessageCodec codec = new JacksonMessageCodec();
            server.publish(new Message(pattern, codec.encode(payload)));
        }

        @Override
        public void close() {
            // 共享 server 生命周期
        }
    }
}
