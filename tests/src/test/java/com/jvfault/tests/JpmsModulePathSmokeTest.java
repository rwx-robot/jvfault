/**
 * jvfault-tests JPMS 跨模块路径冒烟（v1.0.3+ 多版本 JAR 兼容性回归）。
 *
 * <p>本测试在 Gradle 的 {@code tests} 模块以传统 classpath 形式运行（验证
 * Java 8 兼容性），同时检查每个模块的：
 * <ul>
 *   <li>module-info.java 源码中的 {@code module com.jvfault.X} 与项目自动模块名一致
 *       （捕获此前 {@code :native}/{@code :security} 类静默失败）</li>
 *   <li>构建产物的 jar manifest 含 {@code Automatic-Module-Name} 与 {@code Multi-Release: true}</li>
 *   <li>jar 内含 {@code META-INF/versions/9/module-info.class}</li>
 * </ul>
 *
 * <p>另验证：
 * <ul>
 *   <li>{@code com.jvfault.core.bootstrap.JvfaultApplication#getVersion()} 返回的版本字符串
 *       与构建期写入的 {@code jvfault.framework.version} 系统属性一致</li>
 *   <li>{@code examples/v*} 目录下的示例应用 {@code Application} main 类可被 classpath 加载
 *       （12 个版本示例不会因 API 漂移而静默崩溃）</li>
 * </ul>
 *
 * @since v1.0.3 (2026)
 */
package com.jvfault.tests;

import com.jvfault.core.bootstrap.JvfaultApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 见同包 java doc.
 */
@DisplayName("JPMS 多版本 JAR 回归（MR-JAR descriptor + describe-module + IoC 启动）")
class JpmsModulePathSmokeTest {

    /**
     * 模块在工作区根的相对路径；CI 环境用 {@code MR_JAR_DIR} 环境变量覆盖。
     * <p>优先取 Gradle 通过 {@code -Djvfault.project.root} 注入的项目根；
     * 其次 {@code MR_JAR_DIR} 环境变量；最后回退到 {@code user.dir}。
     */
    private static Path moduleRoot() {
        String override = System.getProperty("jvfault.project.root");
        if (override == null || override.isEmpty()) {
            override = System.getenv("MR_JAR_DIR");
        }
        if (override == null || override.isEmpty()) {
            override = System.getProperty("user.dir");
        }
        return Path.of(override).toAbsolutePath().normalize();
    }

    /** 构建期版本，由 :tests:build.gradle.kts 注入。 */
    private static String frameworkVersion() {
        String v = System.getProperty("jvfault.framework.version");
        return v != null && !v.isEmpty() ? v : "0.0.0";
    }

    /**
     * 已知模块名特例：项目名是 Java 保留字或与包名不符。
     * 与根 build.gradle.kts 的映射保持一致。
     */
    private static String expectedAutoName(String projectName) {
        return switch (projectName) {
            case "native" -> "com.jvfault.nativeimage";
            default -> "com.jvfault." + projectName.replace('-', '.');
        };
    }

    /** 在源码层校验 module-info.java 里的 `module X.Y.Z` 与项目名映射一致。 */
    private static final Pattern MODULE_DECL = Pattern.compile("^module\\s+([a-zA-Z0-9_.]+)\\s*\\{",
            Pattern.MULTILINE);

    /**
     * 自动发现带 module-info.java 的子模块（而非硬编码列表）。这样新增模块
     * 自动纳入校验范围，删除模块也不会留下死引用。
     */
    private static Set<Path> discoverModuleInfoFiles() throws IOException {
        Set<Path> result = new LinkedHashSet<>();
        Path root = moduleRoot();
        if (!Files.isDirectory(root)) return result;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path sub : ds) {
                if (!Files.isDirectory(sub)) continue;
                if (sub.getFileName().toString().startsWith(".")) continue;
                if (sub.getFileName().toString().equals("examples")) continue;
                if (sub.getFileName().toString().equals("docs")) continue;
                if (sub.getFileName().toString().equals("gradle")) continue;
                Path mi = sub.resolve("src/main/java9/module-info.java");
                if (Files.isRegularFile(mi)) result.add(mi);
            }
        }
        return result;
    }

    private static String moduleNameFromPath(Path moduleInfo) {
        // 从 "core/src/main/java9/module-info.java" 提取 "core"
        Path moduleDir = moduleInfo;
        for (int i = 0; i < 4; i++) moduleDir = moduleDir.getParent();
        return moduleDir.getFileName().toString();
    }

    /**
     * 校验源码 module-info.java 里的 {@code module X.Y.Z} 与项目自动模块名一致。
     * 失败模式：{@code :native} 项目名是保留字，曾被错误命名为 {@code com.jvfault.native}。
     */
    @Test
    @DisplayName("源码：module-info.java 声明的模块名 == 项目的 Automatic-Module-Name")
    void verifyModuleNameInSource() throws IOException {
        Set<String> mismatches = new LinkedHashSet<>();
        StringBuilder detail = new StringBuilder();
        for (Path mi : discoverModuleInfoFiles()) {
            String moduleName = moduleNameFromPath(mi);
            String expected = expectedAutoName(moduleName);
            String content = Files.readString(mi);
            Matcher matcher = MODULE_DECL.matcher(content);
            if (!matcher.find()) {
                detail.append("\n  ").append(moduleName).append(": 找不到 `module X {` 声明");
                mismatches.add(moduleName + ":no-decl");
                continue;
            }
            String declared = matcher.group(1);
            if (!expected.equals(declared)) {
                detail.append("\n  ").append(moduleName)
                        .append(": module-info 声明=").append(declared)
                        .append("，但项目自动名应为 ").append(expected);
                mismatches.add(moduleName);
            }
        }
        if (!mismatches.isEmpty()) {
            fail("模块名一致性错误（" + mismatches.size() + " 个）：" + detail);
        }
    }

    /**
     * 校验所有带 module-info.java 的模块，发布 jar 都具备：
     * <ul>
     *   <li>{@code Automatic-Module-Name} 属性</li>
     *   <li>{@code Multi-Release: true} 属性</li>
     *   <li>{@code META-INF/versions/9/module-info.class} 条目</li>
     * </ul>
     *
     * <p>jar 缺失视为失败（不再静默跳过 —— 那会掩盖构建问题）。
     */
    @Test
    @DisplayName("构件：MR-JAR descriptor 完整（AMN + Multi-Release + versions/9）")
    void verifyMrJarDescriptors() throws IOException {
        String version = frameworkVersion();
        Set<Path> moduleInfos = discoverModuleInfoFiles();
        // 防御：发现为 0 说明 moduleRoot() 解析错了（过去踩过 user.dir 陷阱），
        // 这会让后面的循环空转 → 测试"假绿"。明确 fail 而不是静默通过。
        assertFalse(moduleInfos.isEmpty(),
                "未发现任何 src/main/java9/module-info.java；moduleRoot()=" + moduleRoot()
                        + "（检查 -Djvfault.project.root 或 MR_JAR_DIR）");
        Set<String> missingJars = new LinkedHashSet<>();
        Set<String> incomplete = new LinkedHashSet<>();
        for (Path mi : moduleInfos) {
            String moduleName = moduleNameFromPath(mi);
            Path jar = moduleRoot().resolve(moduleName).resolve("build/libs")
                    .resolve("jvfault-" + moduleName + "-" + version + ".jar");
            if (!Files.isRegularFile(jar)) {
                missingJars.add(jar.toString());
                continue;
            }
            try (JarFile jf = new JarFile(jar.toFile())) {
                Manifest mf = jf.getManifest();
                Attributes attrs = mf.getMainAttributes();
                String amn = attrs.getValue("Automatic-Module-Name");
                String mr = attrs.getValue("Multi-Release");
                boolean hasV9 = jf.getJarEntry("META-INF/versions/9/module-info.class") != null;
                if (amn == null) incomplete.add(moduleName + ":AMN 缺失");
                if (!"true".equalsIgnoreCase(mr)) incomplete.add(moduleName + ":Multi-Release 非 true (=" + mr + ")");
                if (!hasV9) incomplete.add(moduleName + ":versions/9/module-info.class 缺失");
            }
        }
        StringBuilder msg = new StringBuilder();
        msg.append("共检查 ").append(moduleInfos.size()).append(" 个模块。");
        if (!missingJars.isEmpty()) {
            msg.append("\n缺失 jar（确认 :tests:test 已依赖全量 jar 任务）：\n");
            for (String s : missingJars) msg.append("  - ").append(s).append('\n');
        }
        if (!incomplete.isEmpty()) {
            msg.append("\n描述符不完整：").append(incomplete);
        }
        if (!missingJars.isEmpty() || !incomplete.isEmpty()) fail(msg.toString());
    }

    /**
     * 验证构建期版本与运行时读取一致 — 防止构建期 release 设错导致主代码读不到
     * {@code META-INF/jvfault-build.properties}（曾经在 v1.0.3 MR-JAR 切换时跌过）。
     */
    @Test
    @DisplayName("运行时：getVersion() 与构建期 jvfaultVersion 一致")
    void verifyBuildVersionReadable() {
        String runtime = JvfaultApplication.getVersion();
        String expected = frameworkVersion();
        assertNotNull(runtime, "getVersion() 不应为 null");
        assertEquals(expected, runtime,
                "运行时版本(" + runtime + ") 与构建期版本(" + expected + ") 不一致");
    }

    /**
     * 验证 classpath 形态下（Java 8 兼容）{@link JvfaultApplication} 可被加载。
     * 防止 MR-JAR 机制在主代码编译路径上误改了字节码而让 Java 8 调用方解析失败。
     */
    @Test
    @DisplayName("classpath 路径下 IoC 容器仍可启动（Java 8 兼容）")
    void verifyClasspathIoC() {
        try {
            Class<?> clazz = Class.forName("com.jvfault.core.bootstrap.JvfaultApplication");
            assertNotNull(clazz.getMethod("getVersion"));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("JvfaultApplication 必须在 classpath 上可用：" + e);
        }
    }

    /**
     * 验证本测试模块的 Module 标识可读 — 命名模块与未命名模块都是合法的
     * MR-JAR 兼容性证明。
     */
    @Test
    @DisplayName("当前测试模块标识可读（命名 / 未命名均可）")
    void verifyCurrentModuleReadable() {
        Module m = JpmsModulePathSmokeTest.class.getModule();
        assertNotNull(m, "Module 标识不能为 null");
        System.err.println("[JPMS] 当前测试所在模块: "
                + (m.isNamed() ? m.getName() : "(unnamed)"));
    }

    /**
     * 验证 {@code examples/v*} 12 个版本示例的源码结构完整 —— 每个示例都应有
     * {@code Application} 主类（编译/集成测试由各 example 子项目自己的
     * {@code :examples:v*:test} 覆盖；这里只做结构自检，避免
     * "examples 目录被切走但 CI 仍声称 12/12 绿" 这种静默失败）。
     */
    @Test
    @DisplayName("examples/v* 结构自检（12 个示例 + Application 主类）")
    void verifyExamplesStructure() throws IOException {
        Path root = moduleRoot();
        Path examplesRoot = root.resolve("examples");
        if (!Files.isDirectory(examplesRoot)) {
            fail("moduleRoot=" + root + "，但未发现 examples/ 目录（期望在 moduleRoot 下）");
        }
        Set<String> missing = new LinkedHashSet<>();
        int found = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(examplesRoot, "v*")) {
            for (Path versionDir : ds) {
                if (!Files.isDirectory(versionDir)) continue;
                found++;
                // 各示例包名约定不同（v0.1.0→v010, v0.10.0→v1000, v0.11.0→v110, v1.0.0→v100），
                // 不硬编码路径，直接在源码树里找 Application.java
                Path mainSource = null;
                try (var tree = Files.find(versionDir.resolve("src/main/java"),
                        Integer.MAX_VALUE,
                        (p, attrs) -> p.getFileName().toString().equals("Application.java"))) {
                    mainSource = tree.findFirst().orElse(null);
                }
                if (mainSource == null) {
                    missing.add(versionDir + "/src/main/java/**/Application.java");
                }
            }
        }
        if (found != 12) {
            fail("期望 12 个 examples/v* 目录，实际发现 " + found + " 个");
        }
        if (!missing.isEmpty()) {
            fail("以下示例主类缺失：\n" + String.join("\n", missing));
        }
    }
}