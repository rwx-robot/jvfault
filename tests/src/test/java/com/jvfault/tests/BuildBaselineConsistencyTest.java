package com.jvfault.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
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
 * 构建基线一致性回归 —— 防止「JDK 分层」在代码与文档之间漂移。
 *
 * <p><b>背景（2026-09-24 实测发现的真实缺陷）</b>：
 * 框架对外宣称「基线 JDK 8」，但其实有 6 个发布模块在各自的 {@code build.gradle.kts}
 * 里覆盖了 {@code options.release}：
 * <ul>
 *   <li>{@code ai}/{@code rag}/{@code mcp}/{@code compliance}/{@code migration} → 17</li>
 *   <li>{@code virtualthreads} → 21</li>
 * </ul>
 * 把它们放到真正的 Java 8 JVM 上会直接抛 {@code UnsupportedClassVersionError}。
 * 而 README 当时<b>只标了 ai/rag/mcp 的 (JDK 17)</b>，把 compliance/migration/virtualthreads
 * 全漏掉了 —— 用户按 README 选型就会踩雷。
 *
 * <p>本测试把这个「隐性契约」固化成机器可校验的规则，三重防护：
 * <ol>
 *   <li>{@link #buildFilesMatchExpectedRelease()} —— 各模块实际的 {@code options.release}
 *       必须与期望表一致。谁偷偷升/降 baseline，测试立刻红。</li>
 *   <li>{@link #readmeMarksEveryHighJdkModule()} —— 所有 release &gt; 8 的<b>已发布</b>模块，
 *       README 模块清单里必须标注 {@code (JDK n)}。防止再次「漏标」。</li>
 *   <li>{@link #readmeJdkTierTableIsAccurate()} —— README「JDK 需求分层」表里列出的模块，
 *       必须与实际 release 吻合。防止文档表与实际脱节。</li>
 * </ol>
 *
 * <p><b>改动基线时怎么办</b>：改完 {@code build.gradle.kts} 后同步更新本文件的
 * {@link #EXPECTED} 表和 README 两处，测试自然变绿 —— 这是<b>故意</b>设的摩擦，
 * 避免有人单独改一边。
 *
 * @since v1.0.8 (2026)
 */
@DisplayName("构建基线一致性（防止 JDK 分层漂移）")
class BuildBaselineConsistencyTest {

    /** 默认 baseline —— 根 build.gradle.kts 的 options.release。 */
    private static final int DEFAULT_RELEASE = 8;

    /**
     * 显式偏离默认 baseline 的模块。未列出的模块一律视为 {@link #DEFAULT_RELEASE}。
     *
     * <p><b>改这里必须同步</b>：README「JDK 需求分层」表 + 模块清单的 {@code (JDK n)} 标注。
     */
    private static final Map<String, Integer> EXPECTED = new LinkedHashMap<>();

    static {
        // —— 已发布模块：README 必须标注 ——
        EXPECTED.put("ai", 17);
        EXPECTED.put("rag", 17);
        EXPECTED.put("mcp", 17);
        EXPECTED.put("compliance", 17);
        EXPECTED.put("migration", 17);
        EXPECTED.put("virtualthreads", 21);
        // —— 非发布产物：不进 README 清单 ——
        EXPECTED.put("tests", 17);                 // 测试套件本身
        EXPECTED.put("examples/v0.8.0", 21);       // 依赖 virtualthreads 的示例
    }

    /** {@code tests} 与 {@code examples/*} 不对外发布，README 无需为它们标 JDK。 */
    private static boolean isPublished(String moduleName) {
        return !moduleName.equals("tests") && !moduleName.startsWith("examples/");
    }

    private static final Pattern RELEASE_DECL =
            Pattern.compile("options\\.release\\s*=\\s*(\\d+)");

    private static Path projectRoot() {
        String override = System.getProperty("jvfault.project.root");
        String base = (override != null && !override.isEmpty())
                ? override : System.getProperty("user.dir");
        return Paths.get(base).toAbsolutePath().normalize();
    }

    /** 扫描各模块的 build.gradle.kts，提取实际声明的 options.release。 */
    private Map<String, Integer> scanActualRelease() throws IOException {
        Map<String, Integer> actual = new LinkedHashMap<>();
        Path root = projectRoot();
        if (!Files.isDirectory(root)) return actual;
        try (DirectoryStream<Path> top = Files.newDirectoryStream(root)) {
            for (Path sub : top) {
                if (!Files.isDirectory(sub)) continue;
                String name = sub.getFileName().toString();
                if (name.startsWith(".")) continue;
                Path bf = sub.resolve("build.gradle.kts");
                if (Files.isRegularFile(bf)) {
                    Integer r = extractRelease(bf);
                    if (r != null) actual.put(name, r);
                }
                // examples 是二级结构（examples/vX.Y.Z/）
                if (name.equals("examples")) {
                    try (DirectoryStream<Path> exs = Files.newDirectoryStream(sub)) {
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
        if (!m.find()) return null;
        return Integer.valueOf(m.group(1));
    }

    /**
     * 防护 1：各模块实际的 options.release 必须与期望表一致。
     * 捕获「有人改了 build.gradle.kts 却忘了同步文档」。
     */
    @Test
    @DisplayName("构建：各模块 options.release 与期望表一致（偷偷升降即失败）")
    void buildFilesMatchExpectedRelease() throws IOException {
        Map<String, Integer> actual = scanActualRelease();
        assertFalse(actual.isEmpty(),
                "未扫描到任何 build.gradle.kts；projectRoot=" + projectRoot());

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Integer> e : actual.entrySet()) {
            String mod = e.getKey();
            int actualRel = e.getValue();
            int expectedRel = EXPECTED.getOrDefault(mod, DEFAULT_RELEASE);
            if (actualRel != expectedRel) {
                problems.add("  " + mod + ": 实际 release=" + actualRel
                        + "，期望 " + expectedRel
                        + "（若确需调整，请同步 BuildBaselineConsistencyTest.EXPECTED 与 README）");
            }
        }
        // 反向：期望表里列了但实际 build 文件不存在（模块被删/改名）
        for (String mod : EXPECTED.keySet()) {
            if (!actual.containsKey(mod)) {
                problems.add("  " + mod + ": 期望表里声明了，但未扫描到其 build.gradle.kts（模块改名/删除？）");
            }
        }
        if (!problems.isEmpty()) {
            fail("JDK baseline 漂移，共 " + problems.size() + " 处：\n"
                    + String.join("\n", problems));
        }
    }

    /**
     * 防护 2：所有 release &gt; 8 的<b>已发布</b>模块，README 模块清单必须标注 {@code (JDK n)}。
     * 这正是本次发现的缺陷 —— compliance/migration/virtualthreads 当时都被漏标。
     */
    @Test
    @DisplayName("文档：高 JDK 需求的已发布模块在 README 清单里标注了 (JDK n)")
    void readmeMarksEveryHighJdkModule() throws IOException {
        Map<String, Integer> actual = scanActualRelease();
        Path readme = projectRoot().resolve("README.md");
        assertTrue(Files.isRegularFile(readme), "README.md 不存在：" + readme);
        List<String> lines = Files.readAllLines(readme, StandardCharsets.UTF_8);

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> e : actual.entrySet()) {
            String mod = e.getKey();
            int rel = e.getValue();
            if (rel <= DEFAULT_RELEASE) continue;   // 默认 baseline 无需标注
            if (!isPublished(mod)) continue;        // 非发布产物不管
            if (!readmeLineMarks(lines, mod, rel)) {
                missing.add("  " + mod + "（release=" + rel + "）在 README 模块清单里缺 (JDK " + rel + ") 标注");
            }
        }
        if (!missing.isEmpty()) {
            fail("README 漏标高 JDK 模块，共 " + missing.size() + " 处：\n"
                    + String.join("\n", missing)
                    + "\n修复：在 README 模块清单里给该模块补上 (JDK n) 标注。");
        }
    }

    /**
     * 防护 3：README「JDK 需求分层」表里写的模块，必须真的使用该 release；
     * 且实际 release &gt; 8 的已发布模块都必须出现在该表中。
     */
    @Test
    @DisplayName("文档：README「JDK 需求分层」表与实际 release 吻合")
    void readmeJdkTierTableIsAccurate() throws IOException {
        Map<String, Integer> actual = scanActualRelease();
        Path readme = projectRoot().resolve("README.md");
        List<String> lines = Files.readAllLines(readme, StandardCharsets.UTF_8);

        // 定位「JDK 需求分层」章节的表格区域。
        // 必须匹配 **章节标题行**（## 开头）—— README 开头引言里就出现过
        // 「详见下方「JDK 需求分层」」这句话，若只用 contains() 会命中引言，
        // 导致收集到错误的表格（曾经让本测试抓到「维度/现状」概览表，
        // 报出一堆看似合理实则张冠李戴的错误）。
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("## ") && t.contains("JDK 需求分层")) { start = i; break; }
        }
        assertTrue(start >= 0, "README 缺少「JDK 需求分层」章节（需为 ## 级标题）");

        List<String> tableRows = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.startsWith("## ") && i > start) break;   // 下一章节，停
            if (l.trim().startsWith("|")) tableRows.add(l);
        }
        assertFalse(tableRows.isEmpty(),
                "「JDK 需求分层」章节里没有表格（定位到第 " + (start + 1) + " 行）");

        // 表头行 + 分隔行（各可能出现一次），其余为数据行
        List<String> dataRows = new ArrayList<>();
        for (String row : tableRows) {
            String cells[] = row.split("\\|");
            String first = cells.length > 1 ? cells[1].trim() : "";
            if (first.isEmpty() || first.startsWith("---") || first.startsWith(":-")
                    || first.startsWith("需求 JDK")) {
                continue;   // 表头/分隔行
            }
            dataRows.add(row);
        }
        assertFalse(dataRows.isEmpty(), "「JDK 需求分层」表格没有数据行");

        List<String> problems = new ArrayList<>();

        // 3a. 每个 release>8 的已发布模块，必须出现在表中且 JDK 列一致
        for (Map.Entry<String, Integer> e : actual.entrySet()) {
            String mod = e.getKey();
            int rel = e.getValue();
            if (rel <= DEFAULT_RELEASE) continue;
            if (!isPublished(mod)) continue;

            boolean listed = false;
            for (String row : dataRows) {
                String cells[] = row.split("\\|");
                if (cells.length < 4) continue;
                Integer jdk = parseJdk(cells[1]);
                if (jdk == null) continue;
                String modCell = cells[2].trim();
                if (!containsModule(modCell, mod)) continue;
                listed = true;
                if (jdk != rel) {
                    problems.add("  " + mod + ": README 分层表标 JDK " + jdk
                            + "，实际 build.gradle.kts 是 " + rel);
                }
            }
            if (!listed) {
                problems.add("  " + mod + ": 实际 release=" + rel
                        + "，但 README 分层表里没列出它");
            }
        }

        // 3b. 表里说某模块用某 JDK，但 build 文件里其实不是 —— 反向校验
        for (String row : dataRows) {
            String cells[] = row.split("\\|");
            if (cells.length < 4) continue;
            Integer jdk = parseJdk(cells[1]);
            if (jdk == null || jdk <= DEFAULT_RELEASE) continue;  // 8/9 行由 3a 覆盖
            for (String mod : EXPECTED.keySet()) {
                if (!isPublished(mod)) continue;
                if (!containsModule(cells[2].trim(), mod)) continue;
                int real = actual.getOrDefault(mod, DEFAULT_RELEASE);
                if (real != jdk) {
                    problems.add("  " + mod + ": 分层表写 JDK " + jdk
                            + "，build.gradle.kts 实际 " + real);
                }
            }
        }

        if (!problems.isEmpty()) {
            fail("README「JDK 需求分层」表与实际不符，共 " + problems.size() + " 处：\n"
                    + String.join("\n", problems));
        }
    }

    /**
     * 从表格的 JDK 单元格里提取数字。
     * 单元格可能是 {@code **8**（默认）}、{@code **17**}、{@code 21} 等形态，
     * 所以只取<b>第一个数字</b>，不能整串 parseInt（会抛 NumberFormatException）。
     */
    private static Integer parseJdk(String cell) {
        Matcher m = Pattern.compile("(\\d+)").matcher(cell);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /**
     * README 模块清单的某一行是否在描述里标注了 {@code (JDK n)}。
     * 清单行的格式是「模块名列表」+ 2 个以上空格 + 「描述」。
     */
    private static boolean readmeLineMarks(List<String> lines, String module, int release) {
        String want = "(JDK " + release + ")";
        for (String line : lines) {
            String[] parts = line.split("\\s{2,}", 2);
            if (parts.length < 2) continue;
            if (!containsModule(parts[0].trim(), module)) continue;
            // 同一行模块很多（如 "ai/rag/mcp"），命中了就检查描述里的 JDK 标注
            return parts[1].contains(want);
        }
        return false;
    }

    /**
     * 单元格里可能是一组模块（{@code ai/rag/mcp} 或 {@code compliance/migration}），
     * 且被 markdown 装饰符包裹（`` `ai` ``、**bold**）。比较前先剥掉这些符号 ——
     * 反引号/星号是文档的正当写法，不该导致测试误判。
     */
    private static boolean containsModule(String cell, String module) {
        String normalized = cell.replace("`", "").replace("*", "");
        for (String token : normalized.split("[/,、\\s]+")) {
            if (token.equals(module)) return true;
        }
        return false;
    }
}
