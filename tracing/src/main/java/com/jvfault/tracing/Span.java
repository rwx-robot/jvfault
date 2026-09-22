package com.jvfault.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Span —— 一次可观测的工作单元（name、context、attributes、status、events）
 * 对应 OpenTelemetry: Span
 *
 * <p>时间语义：
 * <ul>
 *   <li>startNanos/endNanos 基于 {@link System#nanoTime()} 单调时钟，
 *       用于计算 {@link #elapsedMs()}</li>
 *   <li>事件时间戳使用毫秒纪元时间（epoch millis）</li>
 * </ul>
 *
 * <p>Span 只能通过 {@link Tracer#spanBuilder(String)} 创建；
 * {@link #end()} 时通过注册的监听回调通知 {@link Tracer} 导出。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public final class Span {

    /** Span 状态 */
    public enum Status {
        /** 未设置（默认） */
        UNSET,
        /** 成功 */
        OK,
        /** 失败 */
        ERROR
    }

    /** Span 事件 —— 带时间戳的 name + attributes */
    public static final class Event {

        private final String name;
        private final long epochMillis;
        private final Map<String, Object> attributes;

        Event(String name, long epochMillis, Map<String, Object> attributes) {
            this.name = name;
            this.epochMillis = epochMillis;
            this.attributes = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Object>(
                            attributes != null ? attributes : new LinkedHashMap<String, Object>()));
        }

        public String getName() {
            return name;
        }

        /** 事件时间戳（epoch millis） */
        public long getEpochMillis() {
            return epochMillis;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return "Event{name='" + name + "', epochMillis=" + epochMillis + ", attributes=" + attributes + "}";
        }
    }

    private final String name;
    private final TraceContext context;
    private final long startNanos;
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    private final List<Event> events = new ArrayList<Event>();
    private final AtomicBoolean ended = new AtomicBoolean(false);

    private volatile Status status = Status.UNSET;
    private volatile long endNanos;
    private volatile Consumer<Span> endListener;

    /**
     * 包内构造：由 {@link SpanBuilder} 创建
     */
    Span(String name, TraceContext context, long startNanos) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("span name 不能为空");
        }
        if (context == null) {
            throw new IllegalArgumentException("span context 不能为空");
        }
        this.name = name;
        this.context = context;
        this.startNanos = startNanos;
    }

    /**
     * 包内：由 {@link SpanBuilder} 注入结束回调（Tracer 导出）
     */
    void setEndListener(Consumer<Span> endListener) {
        this.endListener = endListener;
    }

    // ============ 记录 API ============

    /**
     * 设置属性（end 后调用被忽略）
     */
    public Span setAttribute(String key, Object value) {
        if (ended.get() || key == null || key.isEmpty()) {
            return this;
        }
        synchronized (attributes) {
            attributes.put(key, value);
        }
        return this;
    }

    /**
     * 添加事件（end 后调用被忽略）
     */
    public Span addEvent(String name) {
        return addEvent(name, null);
    }

    /**
     * 添加带属性的事件（时间戳为当前毫秒纪元时间；end 后调用被忽略）
     */
    public Span addEvent(String name, Map<String, Object> attributes) {
        if (ended.get() || name == null || name.isEmpty()) {
            return this;
        }
        synchronized (events) {
            events.add(new Event(name, System.currentTimeMillis(), attributes));
        }
        return this;
    }

    /**
     * 设置状态（end 后调用被忽略）
     */
    public Span setStatus(Status status) {
        if (ended.get() || status == null) {
            return this;
        }
        this.status = status;
        return this;
    }

    // ============ 结束与度量 ============

    /**
     * 结束 Span（endNanos = System.nanoTime()），并通知 Tracer 导出
     */
    public void end() {
        end(System.nanoTime());
    }

    /**
     * 以指定结束时间结束 Span（与 {@link #getStartNanos()} 同基准：System.nanoTime()）；
     * 重复 end 被忽略（幂等）
     */
    public void end(long endNanos) {
        if (!ended.compareAndSet(false, true)) {
            return;
        }
        this.endNanos = endNanos;
        Consumer<Span> listener = this.endListener;
        if (listener != null) {
            try {
                listener.accept(this);
            } catch (RuntimeException ignored) {
                // 导出失败不应影响业务链路
            }
        }
    }

    /**
     * 是否已结束
     */
    public boolean isEnded() {
        return ended.get();
    }

    /**
     * 已耗时毫秒数（未结束时按当前时刻计算，向下取整，最小为 0）
     */
    public long elapsedMs() {
        long end = ended.get() ? endNanos : System.nanoTime();
        return Math.max(0L, (end - startNanos) / 1_000_000L);
    }

    // ============ 内省 ============

    public String getName() {
        return name;
    }

    public TraceContext getContext() {
        return context;
    }

    public long getStartNanos() {
        return startNanos;
    }

    /**
     * 结束时间（nanoTime 基准；仅 {@link #isEnded()} 为 true 后有意义）
     */
    public long getEndNanos() {
        return endNanos;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * 属性快照（只读，保持插入顺序）
     */
    public Map<String, Object> getAttributes() {
        synchronized (attributes) {
            return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
        }
    }

    /**
     * 事件快照（只读，按添加顺序）
     */
    public List<Event> getEvents() {
        synchronized (events) {
            return Collections.unmodifiableList(new ArrayList<Event>(events));
        }
    }
}
