package com.jvfault.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-migration 核心测试
 *
 * @since v1.0.0 (2026)
 */
@DisplayName("Migration 模块测试")
class MigrationModuleTest {

    @TempDir
    Path dir;

    @Test
    @DisplayName("内置规则表覆盖关键映射")
    void testDefaults() {
        List<RewriteRule> rules = RewriteRule.defaults();
        assertTrue(rules.size() >= 8);
        assertTrue(rules.stream().anyMatch(r -> r.from().contains("@Autowired")));
        assertTrue(rules.stream().anyMatch(r -> r.from().contains("@GetMapping")));
    }

    @Test
    @DisplayName("扫描源码并产出发现")
    void testAnalyze() throws Exception {
        Path src = dir.resolve("Legacy.java");
        Files.writeString(src, """
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.web.bind.annotation.GetMapping;
                class Legacy {
                    @Autowired X x;
                    @GetMapping("/a") String a() { return ""; }
                }
                """);

        MigrationAnalyzer analyzer = MigrationAnalyzer.withDefaults();
        List<MigrationAnalyzer.Finding> findings = analyzer.analyze(dir);
        assertEquals(2, findings.size(), "@Autowired 与 @GetMapping 应各命中一次");
        assertEquals("@Autowired", findings.get(0).matchedToken());
    }

    @Test
    @DisplayName("Markdown 报告输出")
    void testReport() throws Exception {
        Path src = dir.resolve("App.java");
        Files.writeString(src, "@SpringBootApplication class App {}");
        MigrationAnalyzer analyzer = MigrationAnalyzer.withDefaults();
        String report = analyzer.report(analyzer.analyze(dir));
        assertTrue(report.contains("# jvfault 迁移报告"));
        assertTrue(report.contains("@SpringBootApplication"));
        assertTrue(report.contains("@Module"));
    }
}
