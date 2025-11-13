package com.jvfault.logging;

/**
 * 日志级别枚举 —— 用于 LogLevelRegistry 与 LogEntry 的级别建模
 *
 * <p>严重程度递增：TRACE(0) &lt; DEBUG(1) &lt; INFO(2) &lt; WARN(3) &lt; ERROR(4)。
 *
 * @since v0.10.0 (2024)
 * @author jvfault team
 */
public enum LogLevel {

    /** 追踪级别 - 最详细的诊断信息 */
    TRACE(0),

    /** 调试级别 - 开发期诊断信息 */
    DEBUG(1),

    /** 信息级别 - 常规运行信息 */
    INFO(2),

    /** 警告级别 - 潜在问题 */
    WARN(3),

    /** 错误级别 - 运行时错误 */
    ERROR(4);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    /**
     * 严重程度数值（越大越严重）
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * 当前级别在指定生效级别下是否允许输出
     *
     * <p>规则：当前级别严重程度 &gt;= 生效级别严重程度时允许输出；
     * 生效级别为 null 表示未覆盖，交由底层 slf4j 决定。
     *
     * @param effectiveLevel 生效级别（可为 null）
     * @return 是否允许输出
     */
    public boolean enabledAt(LogLevel effectiveLevel) {
        if (effectiveLevel == null) {
            return true;
        }
        return this.severity >= effectiveLevel.severity;
    }
}
