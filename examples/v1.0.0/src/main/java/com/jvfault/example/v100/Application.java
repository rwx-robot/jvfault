package com.jvfault.example.v100;

import com.jvfault.compliance.AuditTrail;
import com.jvfault.compliance.DataMasker;
import com.jvfault.migration.MigrationAnalyzer;
import com.jvfault.ops.GracefulShutdown;
import com.jvfault.ops.Health;
import com.jvfault.ops.HealthAggregator;
import com.jvfault.security.crypto.PasswordHasher;
import com.jvfault.security.jwt.JwtService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v1.0.0 示例入口：安全（JWT / 口令哈希）、合规（审计 / 脱敏）、
 * 迁移分析、运维（健康检查 / 优雅关闭）。
 *
 * @since v1.0.0 (2026)
 */
public class Application {

    public static void main(String[] args) throws Exception {
        System.out.println("== jvfault v1.0.0: 安全 / 合规 / 迁移 / 运维 ==");

        security();
        compliance();
        migration();
        operations();

        System.out.println("== 完成 ==");
    }

    // ---------- 1. 安全 ----------
    private static void security() {
        System.out.println("  [security] 口令哈希 (PBKDF2)");
        PasswordHasher hasher = new PasswordHasher();
        String stored = hasher.hash("s3cret-pass");
        System.out.println("    stored=" + stored.substring(0, 24) + "...");
        System.out.println("    正确口令校验=" + hasher.verify("s3cret-pass", stored)
                + "，错误口令校验=" + hasher.verify("wrong", stored));

        System.out.println("  [security] JWT 签发与校验 (HS256)");
        JwtService jwt = new JwtService("jvfault-v100-demo-secret", 3600L);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "1001");
        claims.put("role", "admin");
        String token = jwt.issue(claims);
        Map<String, Object> parsed = jwt.verify(token);
        System.out.println("    token=" + token.substring(0, 32) + "...");
        System.out.println("    校验通过: role=" + parsed.get("role") + ", exp=" + parsed.get("exp"));
        System.out.println("    篡改签名 -> " + jwt.verify(token.substring(0, token.length() - 4) + "AAAA"));
        System.out.println("    已过期   -> " + jwt.verify(jwt.issue(claims, -1L)));
    }

    // ---------- 2. 合规 ----------
    private static void compliance() throws Exception {
        System.out.println("  [compliance] 审计日志 (JSONL 只追加)");
        Path auditFile = Files.createTempFile("jvfault-audit", ".jsonl");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("orderId", "A-2026-0001");
        details.put("amount", 1990);
        try (AuditTrail trail = new AuditTrail(auditFile)) {
            trail.record("alice", "order.create", details);
            trail.record("bob", "order.approve", Map.of("orderId", "A-2026-0001"));
        }
        List<String> lines = Files.readAllLines(auditFile, StandardCharsets.UTF_8);
        System.out.println("    " + lines.size() + " 条事件 -> " + lines.get(0));
        Files.deleteIfExists(auditFile);

        System.out.println("  [compliance] 数据脱敏");
        System.out.println("    手机 13812345678 -> " + DataMasker.maskPhone("13812345678"));
        System.out.println("    邮箱 johnnynode@gmail.com -> " + DataMasker.maskEmail("johnnynode@gmail.com"));
        System.out.println("    卡号 6222021234567890 -> " + DataMasker.mask("6222021234567890", 6));
    }

    // ---------- 3. 迁移 ----------
    private static void migration() throws Exception {
        System.out.println("  [migration] 遗留源码迁移分析");
        // 构造一份遗留风格样例源码，演示规则命中效果
        Path legacy = Files.createTempDirectory("jvfault-legacy");
        Files.writeString(legacy.resolve("LegacyController.java"), String.join("\n",
                "package com.example.legacy;",
                "",
                "import org.springframework.web.bind.annotation.GetMapping;",
                "import org.springframework.web.bind.annotation.RestController;",
                "",
                "@RestController",
                "public class LegacyController {",
                "    @GetMapping(\"/hello\")",
                "    public String hello() {",
                "        return \"hi\";",
                "    }",
                "}",
                ""));
        MigrationAnalyzer analyzer = MigrationAnalyzer.withDefaults();
        List<MigrationAnalyzer.Finding> findings = analyzer.analyze(legacy);
        System.out.println("    扫描 " + legacy + " 命中 " + findings.size() + " 处");
        for (MigrationAnalyzer.Finding finding : findings) {
            System.out.println("    - " + finding.rule().from() + " → " + finding.rule().to());
        }
        analyzer.report(findings).lines()
                .filter(line -> line.startsWith("- "))
                .forEach(line -> System.out.println("    " + line.trim()));
        deleteRecursively(legacy);
    }

    // ---------- 4. 运维 ----------
    private static void operations() {
        System.out.println("  [ops] 健康检查 (liveness / readiness)");
        HealthAggregator aggregator = new HealthAggregator()
                .register("database", () -> Health.up(Map.of("pool", "hikari")))
                .register("cache", () -> Health.down(Map.of("reason", "connection refused")));
        Health liveness = aggregator.liveness();
        Health readiness = aggregator.readiness();
        System.out.println("    liveness=" + liveness.getStatus() + " " + liveness.getDetails());
        System.out.println("    readiness=" + readiness.getStatus() + " " + readiness.getDetails()
                + "（cache DOWN → 整体不就绪）");

        System.out.println("  [ops] 优雅关闭");
        GracefulShutdown shutdown = new GracefulShutdown().install()
                .addHook(() -> System.out.println("    hook#1 停止接收新请求"))
                .addHook(() -> System.out.println("    hook#2 等待在途请求结束"));
        System.out.println("    已注册关闭钩子，SIGTERM 时按序执行；当前 shuttingDown="
                + shutdown.isShuttingDown());
    }

    private static void deleteRecursively(Path root) throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder())
                    .toArray(Path[]::new)) {
                Files.deleteIfExists(path);
            }
        }
    }
}
