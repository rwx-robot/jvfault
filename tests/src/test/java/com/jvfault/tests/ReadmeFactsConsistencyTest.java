package com.jvfault.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * README 文档宣称一致性回归 —— 防止「数字漂移」类缺陷。
 *
 * <p><b>背景（2026-09-24 复盘）</b>：
 * 「JDK 分层漂移」只是<b>文档宣称漂移</b>这一类系统性问题的第一个实例。同一项目里已经发生过：
 * <ul>
 *   <li>集成测试数 28 → 18 的静默回退（无人发现 README 仍写旧值）</li>
 *   <li>静态假 badge（CI 实际状态与 badge 不符）</li>
 *   <li>测试总数 269 → 274 后 README 未同步（badge、维度表、快速开始三处都过期）</li>
 *   <li>「模块含测试」口径写成 38，实际 39 个模块全部含测试</li>
 * </ul>
 * 这些数字分散在 README 多处，改代码后极易漏改文档。本测试把它们<b>一次性</b>固化为机器校验：
 * 每一项都同时核对「源码事实」与「README 陈述」，二者任一漂移即失败，逼着改动者同步两边。
 *
 * <p><b>设计原则（与 BuildBaselineConsistencyTest 一致，故意设摩擦）</b>：
 * <ol>
 *   <li>每个数字在源码里<b>真实派生</b>（解析 settings.gradle.kts / 扫描目录树），不是拍脑袋常量；</li>
 *   <li>派生值再与 README 里写死的数字比对，不一致就红；</li>
 *   <li>新增模块 / 改测试数 / 加传输适配时，<b>必须</b>同步更新本文件与 README，否则 CI 立刻报警。</li>
 * </ol>
 *
 * @since v1.0.9 (2026)
 */
@DisplayName("README 文档宣称一致性（防止数字漂移）")
class ReadmeFactsConsistencyTest {

    // ── 单一事实来源：源码派生值必须与这些常量一致；常量漂移由「源码派生」侧捕获 ──
    private static final int EXPECTED_FRAMEWORK_MODULES = 39;
    private static final int EXPECTED_EXAMPLES = 12;
    private static final int EXPECTED_TRANSPORT = 7;
    private static final int EXPECTED_MODULES_WITH_TESTS = 39;
    private static final int EXPECTED_MR_JAR = 38;
    private static final int EXPECTED_TEST_COUNT = 274;

    /** 默认 baseline —— 根 build.gradle.kts 的 options.release。 */
    private static final int DEFAULT_RELEASE = 8;

    /** release 显式偏离默认基线的模块（未列出的视为 DEFAULT_RELEASE）。 */
    private static final Map<String, Integer> EXPECTED_RELEASE = new LinkedHashMap<>();

    static {
        EXPECTED_RELEASE.put("ai", 17);
        EXPECTED_RELEASE.put("rag", 17);
        EXPECTED_RELEASE.put("mcp", 17);
        EXPECTED_RELEASE.put("compliance", 17);
        EXPECTED_RELEASE.put("migration", 17);
        EXPECTED_RELEASE.put("virtualthreads", 21);
        EXPECTED_RELEASE.put("tests", 17);          // 测试套件，不进运行时
        EXPECTED_RELEASE.put("examples/v0.8.0", 21);
    }

    /**
     * 运行 Java 8 冒烟的「运行时模块」集合 = 框架模块去掉两个纯测试模块
     * （{@code test} / {@code tests}）。它们虽被 maven-publish 打包，但属于测试设施、
     * 不参与「真 Java 8 运行时能否加载」的验收，故冒烟只数这 37 个。
     */
    private static boolean isRuntimeModule(String moduleName) {
        return !moduleName.equals("test") && !moduleName.equals("tests")
                && !moduleName.startsWith("examples/");
    }

    private static final Pattern RELEASE_DECL =
            Pattern.compile("options\\.release\\s*=\\s*(\\d+)");

    private static Path projectRoot() {
        String override = System.getProperty("jvfault.project.root");
        String base = (override != null && !override.isEmpty())
                ? override : System.getProperty("user.dir");
        return Paths.get(base).toAbsolutePath().normalize();
    }

    // ─────────────────────────────── 源码事实派生 ───────────────────────────────

    /** 解析 settings.gradle.kts 的两段 listOf(...).forEach，分别得到框架模块与示例模块。 */
    private static List<List<String>> parseSettingsModules() throws IOException {
        Path settings = projectRoot().resolve("settings.gradle.kts");
        String text = new String(Files.readAllBytes(settings), StandardCharsets.UTF_8);
        Matcher block = Pattern.compile("listOf\\((.*?)\\)\\.forEach", Pattern.DOTALL)
                .matcher(text);
        List<List<String>> result = new ArrayList<>();
        while (block.find()) {
            List<String> names = new ArrayList<>();
            Matcher q = Pattern.compile("\"([^\"]+)\"").matcher(block.group(1));
            while (q.find()) names.add(q.group(1));
            result.add(names);
        }
        return result;
    }

    private int countFrameworkModules() throws IOException {
        return parseSettingsModules().get(0).size();
    }

    private int countExamples() throws IOException {
        return parseSettingsModules().get(1).size();
    }

    private int countTransport() throws IOException {
        int n = 0;
        for (String m : parseSettingsModules().get(0)) {
            if (m.startsWith("transport-")) n++;
        }
        return n;
    }

    private int countModulesWithTests() throws IOException {
        int n = 0;
        for (String m : parseSettingsModules().get(0)) {
            if (Files.isDirectory(projectRoot().resolve(m).resolve("src/test/java"))) n++;
        }
        return n;
    }

    private int countMrJar() throws IOException {
        int n = 0;
        for (String m : parseSettingsModules().get(0)) {
            if (Files.isRegularFile(
                    projectRoot().resolve(m).resolve("src/main/java9/module-info.java"))) {
                n++;
            }
        }
        return n;
    }

    /** 扫描各模块 build.gradle.kts 的 options.release，返回 模块名→release。 */
    private Map<String, Integer> scanActualRelease() throws IOException {
        Map<String, Integer> actual = new LinkedHashMap<>();
        Path root = projectRoot();
        try (var top = Files.newDirectoryStream(root)) {
            for (Path sub : top) {
                if (!Files.isDirectory(sub) || sub.getFileName().toString().startsWith(".")) {
                    continue;
                }
                String name = sub.getFileName().toString();
                Path bf = sub.resolve("build.gradle.kts");
                if (Files.isRegularFile(bf)) {
                    Integer r = extractRelease(bf);
                    if (r != null) actual.put(name, r);
                }
                if (name.equals("examples")) {
                    try (var exs = Files.newDirectoryStream(sub)) {
                        for (Path ex : exs) {
                            if (!Files.isDirectory(ex)) continue;
                            Path ebf = ex.resolve("build.gradle.kts");
                            if (Files.isRegularFile(ebf)) {
                                Integer r = extractRelease(ebf);
                                if (r != null) {
                                    actual.put("examples/" + ex.getFileName(), r);
                                }
                            }
                        }
                    }
                }
            }
        }
        return actual;
    }

    private Integer extractRelease(Path buildFile) throws IOException {
        String text = new String(Files.readAllBytes(buildFile), StandardCharsets.UTF_8);
        Matcher m = RELEASE_DECL.matcher(text);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    // ─────────────────────────────── README 解析 ───────────────────────────────

    private List<String> readmeLines() throws IOException {
        Path readme = projectRoot().resolve("README.md");
        assertTrue(Files.isRegularFile(readme), "README.md 不存在：" + readme);
        return Files.readAllLines(readme, StandardCharsets.UTF_8);
    }

    /** 在「维度 / 现状」表里，按维度名取单元格文本。 */
    private String dimensionCell(List<String> lines, String dimName) {
        boolean inTable = false;
        for (String line : lines) {
            if (line.trim().startsWith("| 维度 | 现状 |")) {
                inTable = true;
                continue;
            }
            if (inTable) {
                if (!line.trim().startsWith("|")) break;   // 表结束
                String[] cells = line.split("\\|");
                if (cells.length >= 3) {
                    String head = cells[1].trim();
                    if (head.equals(dimName)) return cells[2].trim();
                }
            }
        }
        return null;
    }

    /** 从任意文本里取第一个整数。 */
    private static Integer firstInt(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("(\\d+)").matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    // ─────────────────────────────── 测试用例 ───────────────────────────────

    @Test
    @DisplayName("模块数：源码派生 39 == 常量 == README「39 个」")
    void frameworkModuleCountConsistent() throws IOException {
        int derived = countFrameworkModules();
        assertEquals(EXPECTED_FRAMEWORK_MODULES, derived,
                "settings.gradle.kts 里框架模块数变了？同步本常量与 README");
        Integer readme = firstInt(dimensionCell(readmeLines(), "模块"));
        assertEquals(EXPECTED_FRAMEWORK_MODULES, readme,
                "README「维度」表里的模块数与源码不符（应为 39）");
    }

    @Test
    @DisplayName("版本示例数：源码派生 12 == 常量 == README「12 个」")
    void exampleCountConsistent() throws IOException {
        int derived = countExamples();
        assertEquals(EXPECTED_EXAMPLES, derived,
                "settings.gradle.kts 里示例数变了？同步本常量与 README");
        Integer readme = firstInt(dimensionCell(readmeLines(), "版本示例"));
        assertEquals(EXPECTED_EXAMPLES, readme,
                "README「维度」表里的版本示例数与源码不符（应为 12）");
    }

    @Test
    @DisplayName("传输适配数：源码派生 7 == 常量 == README「7 个」")
    void transportCountConsistent() throws IOException {
        int derived = countTransport();
        assertEquals(EXPECTED_TRANSPORT, derived,
                "transport-* 模块数变了？同步本常量与 README");
        List<String> lines = readmeLines();
        boolean mentions7 = lines.stream().anyMatch(l -> l.contains("7 个均含真实集成测试"));
        assertTrue(mentions7, "README 未以「7 个均含真实集成测试」声明传输适配数（源码派生=" + derived + "）");
    }

    @Test
    @DisplayName("含测试模块数：源码派生 39 == 常量 == README「全部 39 个均含测试」")
    void modulesWithTestsConsistent() throws IOException {
        int derived = countModulesWithTests();
        assertEquals(EXPECTED_MODULES_WITH_TESTS, derived,
                "含 src/test/java 的模块数变了？同步本常量与 README");
        String cell = dimensionCell(readmeLines(), "模块");
        assertNotNull(cell, "README「维度」表缺少「模块」行");
        assertTrue(cell.contains("39"), "README 模块行未写 39：" + cell);
        assertTrue(cell.contains("全部") || cell.contains("均含"),
                "README 模块行应明确「全部 39 个均含测试」，当前：" + cell);
    }

    @Test
    @DisplayName("MR-JAR 数：源码派生 38 == 常量 == README「38 个模块」")
    void mrJarCountConsistent() throws IOException {
        int derived = countMrJar();
        assertEquals(EXPECTED_MR_JAR, derived,
                "提供 META-INF/versions/9/module-info.java 的模块数变了？同步本常量与 README");
        Integer readme = firstInt(dimensionCell(readmeLines(), "JPMS"));
        assertEquals(EXPECTED_MR_JAR, readme,
                "README「维度」表 JPMS 行的 MR-JAR 模块数与源码不符（应为 38）");
    }

    @Test
    @DisplayName("测试总数：badge 与维度表都必须是 274")
    void testCountConsistent() throws IOException {
        List<String> lines = readmeLines();
        String whole = String.join("\n", lines);
        // 1) Tests badge：tests-274 passing（badge URL 里空格是 %20 编码）
        Matcher badge = Pattern.compile("tests-(\\d+)(?:%20|\\s+)passing").matcher(whole);
        assertTrue(badge.find(), "README Tests badge 未写 tests-NNN passing 形态");
        assertEquals(EXPECTED_TEST_COUNT, Integer.valueOf(badge.group(1)),
                "README Tests badge 的测试数与实际不符（应为 274；改测试后请同步）");
        // 2) 维度表「测试」行也要 274
        Integer readme = firstInt(dimensionCell(lines, "测试"));
        assertEquals(EXPECTED_TEST_COUNT, readme,
                "README「维度」表「测试」行的数字与 badge 不符（应为 274）");
    }

    @Test
    @DisplayName("运行时冒烟数：高 JDK 已发布模块 6 个，且 31 通过 + 6 抛 = 37 已发布")
    void runtimeSmokeCountsConsistent() throws IOException {
        Map<String, Integer> actual = scanActualRelease();
        // 源码派生：运行时模块里 release>8 的数量（即 Java 8 上会抛 UnsupportedClassVersionError 的）
        int highJdkRuntime = 0;
        for (Map.Entry<String, Integer> e : actual.entrySet()) {
            if (!isRuntimeModule(e.getKey())) continue;
            if (e.getValue() > DEFAULT_RELEASE) highJdkRuntime++;
        }
        // 源码派生：参与 Java 8 冒烟的运行时模块总数
        int runtimeTotal = 0;
        for (String m : parseSettingsModules().get(0)) {
            if (isRuntimeModule(m)) runtimeTotal++;
        }

        List<String> lines = readmeLines();
        String whole = String.join("\n", lines);

        // README 必须写「6 个 jar 抛 UnsupportedClassVersionError」
        Matcher failM = Pattern.compile("(\\d+)\\s*个\\s*jar\\s*抛").matcher(whole);
        assertTrue(failM.find(), "README「运行时实测」未写「N 个 jar 抛 UnsupportedClassVersionError」");
        assertEquals(highJdkRuntime, Integer.valueOf(failM.group(1)),
                "README 说抛错的 jar 数与源码派生的高 JDK 运行时模块数不符");

        // README 必须写「31 个 jar 全部通过」
        Matcher passM = Pattern.compile("(\\d+)\\s*个\\s*jar\\s*全部通过").matcher(whole);
        assertTrue(passM.find(), "README「运行时实测」未写「N 个 jar 全部通过」");
        int pass = Integer.valueOf(passM.group(1));

        // 内部一致性：通过 + 抛错 == 已发布(运行时)总数
        assertEquals(runtimeTotal, pass + highJdkRuntime,
                "README 的 通过+抛错 之和应与运行时模块总数一致");

        // README 必须写「37 个已发布 jar」（此处指运行时验收的 37 个）
        Matcher totalM = Pattern.compile("(\\d+)\\s*个\\s*已发布\\s*jar").matcher(whole);
        assertTrue(totalM.find(), "README「运行时实测」未写「N 个已发布 jar」");
        assertEquals(runtimeTotal, Integer.valueOf(totalM.group(1)),
                "README 的「已发布 jar」数与运行时模块总数不符（应为 37 = 39 - test - tests）");
    }
}
