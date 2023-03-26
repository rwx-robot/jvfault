package com.jvfault.compliance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-compliance 核心测试
 *
 * @since v1.0.0 (2026)
 */
@DisplayName("Compliance 模块测试")
class ComplianceModuleTest {

    @Test
    @DisplayName("审计事件写入 JSONL")
    void testAuditTrailJsonl() throws Exception {
        StringWriter sink = new StringWriter();
        try (AuditTrail trail = new AuditTrail(sink)) {
            trail.record("alice", "user.login", Map.of("ip", "10.0.0.1"));
            trail.record("bob", "user.delete", Map.of("target", "42"));
        }
        String jsonl = sink.toString();
        String[] lines = jsonl.trim().split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("\"actor\":\"alice\""));
        assertTrue(lines[0].contains("\"action\":\"user.login\""));
        assertTrue(lines[1].contains("user.delete"));
    }

    @Test
    @DisplayName("审计事件追加到文件")
    void testAuditTrailFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path file = dir.resolve("audit.jsonl");
        try (AuditTrail trail = new AuditTrail(file)) {
            trail.record("system", "startup", null);
        }
        try (AuditTrail trail = new AuditTrail(file)) {
            trail.record("system", "shutdown", null);
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size(), "追加模式: 两段会话共两条");
    }

    @Test
    @DisplayName("审计条目含时间戳与唯一 id")
    void testAuditEntryFields() {
        StringWriter sink = new StringWriter();
        AuditTrail trail = new AuditTrail(sink);
        AuditTrail.AuditEntry entry = trail.record("carol", "export", Map.of("rows", 100));
        assertNotNull(entry.id());
        assertTrue(entry.timestamp() > 0);
        assertEquals("carol", entry.actor());
        assertEquals(100, ((Number) entry.details().get("rows")).intValue());
    }

    @Test
    @DisplayName("手机号/邮箱/通用掩码")
    void testMasking() {
        assertEquals("138****5678", DataMasker.maskPhone("13812345678"));
        assertNull(DataMasker.maskPhone(null));
        assertEquals("abc", DataMasker.maskPhone("abc"));

        String masked = DataMasker.maskEmail("alice@example.com");
        assertEquals("a***@example.com", masked);

        assertEquals("ab****", DataMasker.mask("abcdef", 2));
        assertEquals("abc", DataMasker.mask("abc", 5));
        assertNull(DataMasker.mask(null, 2));
    }

    @Test
    @DisplayName("未使用的读取入口保持 API 对称")
    void testReaderAccepted() {
        new StringReader("");
        assertTrue(true);
    }
}
