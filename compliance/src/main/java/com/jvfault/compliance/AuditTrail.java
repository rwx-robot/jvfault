package com.jvfault.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志 —— 只追加 JSONL 事件流（谁/何时/做了什么）。
 * 对应 roadmap v1.0.0: 合规审计（事件溯源风格）
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public class AuditTrail implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Writer writer;
    private final boolean ownsWriter;

    public AuditTrail(Writer writer) {
        this.writer = writer;
        this.ownsWriter = false;
    }

    public AuditTrail(Path file) throws IOException {
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.ownsWriter = true;
    }

    /**
     * 记录审计事件。
     */
    public synchronized AuditEntry record(String actor, String action, Map<String, Object> details) {
        AuditEntry entry = new AuditEntry(UUID.randomUUID().toString(),
                actor, action, Instant.now().toEpochMilli(),
                details != null ? details : Map.of());
        try {
            writer.write(MAPPER.writeValueAsString(entry));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("审计写入失败", e);
        }
        return entry;
    }

    @Override
    public void close() throws IOException {
        if (ownsWriter) {
            writer.close();
        }
    }

    /** 审计事件 */
    public record AuditEntry(String id, String actor, String action, long timestamp,
                             Map<String, Object> details) {
    }
}
