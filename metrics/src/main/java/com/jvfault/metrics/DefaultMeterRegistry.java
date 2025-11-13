package com.jvfault.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 指标门面注册表。
 * 对应 Micrometer: MeterRegistry（自研轻量子集，保留桥接可能）
 *
 * <p>同名同 tag 的指标复用同一实例（tag 顺序无关）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class DefaultMeterRegistry implements MeterRegistry {

    private final ConcurrentHashMap<String, Meter> meters = new ConcurrentHashMap<>();
    private volatile MeterFilter filter = MeterFilter.allowAll();

    @Override
    public Counter counter(String name, String... tags) {
        return getOrCreate(Counter.class, MeterType.COUNTER, name, tags, () -> new Counter(newId(name, MeterType.COUNTER, tags)));
    }

    @Override
    public Timer timer(String name, String... tags) {
        return getOrCreate(Timer.class, MeterType.TIMER, name, tags, () -> new Timer(newId(name, MeterType.TIMER, tags)));
    }

    @Override
    public DistributionSummary summary(String name, String... tags) {
        return getOrCreate(DistributionSummary.class, MeterType.SUMMARY, name, tags,
                () -> new DistributionSummary(newId(name, MeterType.SUMMARY, tags)));
    }

    @Override
    public Gauge gauge(String name, final Number number, String... tags) {
        return getOrCreate(Gauge.class, MeterType.GAUGE, name, tags,
                () -> new Gauge(newId(name, MeterType.GAUGE, tags), number::doubleValue));
    }

    @Override
    public Gauge gauge(String name, final java.util.function.DoubleSupplier supplier, String... tags) {
        return getOrCreate(Gauge.class, MeterType.GAUGE, name, tags, () -> new Gauge(newId(name, MeterType.GAUGE, tags), supplier));
    }

    @Override
    public void setFilter(MeterFilter filter) {
        this.filter = filter != null ? filter : MeterFilter.allowAll();
    }

    @Override
    public Meter find(String name, String... tags) {
        return meters.get(id(name, tags));
    }

    @Override
    public void clear() {
        meters.clear();
    }

    public List<Meter> getMeters() {
        return meters.values().stream()
                .sorted(java.util.Comparator.comparing((Meter m) -> m.getId().toString()))
                .collect(Collectors.toList());
    }

    private <M extends Meter> M getOrCreate(Class<M> type, MeterType meterType, String name, String[] tags,
                                            Supplier<M> factory) {
        String id = id(name, tags);
        Meter created = meters.computeIfAbsent(id, k -> {
            Meter meter = factory.get();
            if (!filter.accepts(meter.getId())) {
                meter.markNoop();
            }
            return meter;
        });
        if (!type.isInstance(created)) {
            throw new IllegalArgumentException("指标 " + id + " 已注册为 " + created.getType() + " 类型");
        }
        return type.cast(created);
    }

    static Meter.Id newId(String name, MeterType type, String... tags) {
        return new Meter.Id(name, type, toTags(tags));
    }

    private static Map<String, String> toTags(String[] tags) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < tags.length; i += 2) {
            map.put(tags[i], tags[i + 1]);
        }
        return map;
    }

    private static String id(String name, String[] tags) {
        if (tags.length == 0) {
            return name;
        }
        Map<String, String> sorted = new TreeMap<>(toTags(tags));
        StringBuilder sb = new StringBuilder(name);
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append('|').append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

}
