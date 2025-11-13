package com.jvfault.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 运行时日志级别注册表 —— 按 Logger 名称前缀动态调整级别（内存注册表）
 *
 * <p>工作方式：
 * <ul>
 *   <li>{@link #setLevel(String, LogLevel)} 记录"前缀 -&gt; 级别"覆盖项（线程安全）</li>
 *   <li>{@link StructuredLogger} 每次输出前向本注册表查询
 *       {@link #getEffectiveLevel(String)}，最长匹配前缀优先；无覆盖时交由底层 slf4j 决定</li>
 *   <li>{@link #resetAll()} 清空全部覆盖项（恢复默认）</li>
 * </ul>
 *
 * <p>说明：v0.10.0 仅提供进程内注册表；HTTP/运维端点集成由 ops 模块负责。
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public class LogLevelRegistry {

    /** 前缀 -> 级别覆盖项（按 key 有序，支持最长前缀匹配） */
    private final ConcurrentNavigableMap<String, LogLevel> overrides = new ConcurrentSkipListMap<String, LogLevel>();

    /**
     * 设置某个 Logger 名称前缀的级别覆盖
     *
     * @param loggerNamePrefix Logger 名称前缀（不能为空），如 {@code "com.jvfault"}
     * @param level            生效级别
     */
    public void setLevel(String loggerNamePrefix, LogLevel level) {
        if (loggerNamePrefix == null || loggerNamePrefix.isEmpty()) {
            throw new IllegalArgumentException("loggerNamePrefix 不能为空");
        }
        if (level == null) {
            throw new IllegalArgumentException("level 不能为空");
        }
        overrides.put(loggerNamePrefix, level);
    }

    /**
     * 查询 Logger 的生效级别（最长匹配前缀优先）
     *
     * @param loggerName Logger 名称
     * @return 生效级别；无任何前缀覆盖时返回 null（交由底层 slf4j 决定）
     */
    public LogLevel getEffectiveLevel(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }
        // 自上而下寻找最长匹配前缀：floorEntry 给出 <= loggerName 的最大 key，
        // 若它不是前缀则继续向下（严格递减）探测
        String candidate = loggerName;
        while (candidate != null) {
            Map.Entry<String, LogLevel> entry = overrides.floorEntry(candidate);
            if (entry == null) {
                return null;
            }
            if (loggerName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
            Map.Entry<String, LogLevel> lower = overrides.lowerEntry(entry.getKey());
            candidate = lower != null ? lower.getKey() : null;
        }
        return null;
    }

    /**
     * 清空全部级别覆盖（恢复默认行为）
     */
    public void resetAll() {
        overrides.clear();
    }

    /**
     * 当前覆盖项快照（只读，前缀 -&gt; 级别）
     */
    public Map<String, LogLevel> getOverrides() {
        return new ConcurrentSkipListMap<String, LogLevel>(overrides);
    }
}
