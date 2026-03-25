import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * jvfault Java 8 运行时冒烟 —— 在**真正的 Java 8 JVM** 上验证框架可用性。
 *
 * <p>背景：框架宣称「Java 8 基线」，但日常测试全在 JDK 21 上跑
 * （依赖 {@code --release 8} 的编译期检查）。编译期检查能保证 API 兼容，
 * 却无法证明**字节码能被目标 JVM 真正加载执行**。本程序补上这一环。
 *
 * <p>框架基线其实是<b>分层的</b>（见各模块 build.gradle.kts 的 options.release）：
 * <ul>
 *   <li>经典模块（core/web/transport-* 等绝大多数）：{@code --release 8} → major 52</li>
 *   <li>AI 时代模块（ai/mcp/rag/compliance/migration/tests）：{@code --release 17} → major 61</li>
 *   <li>虚拟线程 / Spring Boot Starter（及 examples）：{@code --release 21} → major 65</li>
 * </ul>
 * 因此本程序按<b>逐模块声明的基线</b>做断言，而不是一刀切要求全部 ≤52。
 *
 * <p>验证项：
 * <ol>
 *   <li>当前 JVM 确为 Java 8（{@code java.class.version <= 52}）</li>
 *   <li>{@code JvfaultApplication.getVersion()} 可读（build properties 能加载）</li>
 *   <li><b>逐 jar 扫描 class 文件 major version</b>：每个模块自身的字节码必须
 *       ≤ 其声明基线。这一步<b>不依赖任何三方库</b>，直接读 jar 内
 *       {@code .class} 文件头，因此 platform-reactive / openapi / native / aot /
 *       graphql / sse 这 6 个之前因缺三方依赖而「静默未验证」的模块，
 *       <b>也都能被真 Java 8 字节码级验证</b>。</li>
 *   <li>对<b>基线为 Java 8</b> 的模块，在 Java 8 上真正加载类（链接校验）；
 *       只有 {@code com.jvfault.*} 类出现 {@link UnsupportedClassVersionError}
 *       才算基线违规。三方依赖缺失导致的 {@code NoClassDefFoundError} 不计。</li>
 *   <li>IoC 入口可在 Java 8 上反射调用</li>
 * </ol>
 *
 * <p><b>判定标准</b>：某模块自身类的 {@code major > 该模块基线} 才算违规。
 * 超出基线的高版本层（MR-JAR 的 {@code META-INF/versions/N/}）不视为违规。
 *
 * <p><b>编译</b>：用 JDK 21 的 {@code javac --release 8} 编译（JRE 8 不含 javac）。
 *
 * <p><b>运行</b>：{@code java -cp "<classes>:<jars>:<slf4j>" Java8RuntimeSmoke <jarsDir>}
 *
 * @since v1.0.8 (2026)
 */
public class Java8RuntimeSmoke {

    private static int pass = 0;
    private static int fail = 0;
    private static final List<String> failures = new ArrayList<String>();

    // 各模块声明的字节码基线（major version）。键为 jar 名里的模块段，默认 52（Java 8）。
    // 数据来源：各模块 build.gradle.kts 的 options.release。
    private static final Map<String, Integer> BASELINE = new HashMap<String, Integer>();
    static {
        // --release 17 → major 61
        BASELINE.put("ai", 61);
        BASELINE.put("mcp", 61);
        BASELINE.put("rag", 61);
        BASELINE.put("compliance", 61);
        BASELINE.put("migration", 61);
        BASELINE.put("tests", 61);
        // --release 21 → major 65
        BASELINE.put("virtualthreads", 65);
        BASELINE.put("spring-boot-starter", 65);
        // examples 内 v0.8.0 为 release 21，整体按 21 计（更宽松，不会误报）
        BASELINE.put("examples", 65);
    }

    // jvfault 主代码默认基线：Java 8 = 52
    private static final int DEFAULT_BASELINE = 52;

    // jar 名形如 jvfault-<module>-<version>.jar，module 段可含 '-'
    private static final Pattern JAR = Pattern.compile("^jvfault-(.+)-(\\d+\\.\\d+\\.\\d+)\\.jar$");

    public static void main(String[] args) {
        String jarsDir = args.length > 0 ? args[0] : "jars";

        System.out.println("========== jvfault Java 8 运行时冒烟 ==========");
        System.out.println("java.version        = " + System.getProperty("java.version"));
        System.out.println("java.class.version  = " + System.getProperty("java.class.version"));
        System.out.println("java.vm.name        = " + System.getProperty("java.vm.name"));
        System.out.println("");

        // 1. 确认真的是 Java 8
        check("运行在 Java 8 JVM 上", isJava8(),
                "期望 java.class.version <= 52，实际 " + System.getProperty("java.class.version"));

        // 2. 框架版本可读
        try {
            String version = com.jvfault.core.bootstrap.JvfaultApplication.getVersion();
            ok("JvfaultApplication.getVersion() = " + version);
        } catch (Throwable t) {
            bad("JvfaultApplication.getVersion() 失败", t);
        }

        // 3. 逐 jar 扫描 class 文件 major version（覆盖全部模块，含那 6 个）
        System.out.println("");
        System.out.println("--- 逐 jar 字节码 major version 扫描（按各模块基线断言）---");
        int jarCount = 0;
        int baseScanned = 0;
        int mrLayers = 0;
        File dir = new File(jarsDir);
        File[] jars = dir.listFiles();
        if (jars == null || jars.length == 0) {
            bad("jar 目录为空或不存在: " + dir.getAbsolutePath(), null);
        } else {
            for (File f : jars) {
                String n = f.getName();
                if (!n.endsWith(".jar")) continue;
                if (n.contains("sources") || n.contains("javadoc")) continue;
                jarCount++;
                int[] r = scanJarBytecode(f);
                baseScanned += r[0];
                mrLayers += r[1];
            }
        }
        System.out.println("");
        System.out.println("扫描 " + jarCount + " 个 jar，基类 " + baseScanned
                + " 个，MR-JAR 多版本层类 " + mrLayers + " 个");

        // 4. 仅对「基线为 Java 8」的模块在 Java 8 上真正加载类（链接校验）
        System.out.println("");
        System.out.println("--- 基线 Java 8 模块：在 Java 8 上加载类 ---");
        if (jars != null) {
            for (File f : jars) {
                String n = f.getName();
                if (!n.endsWith(".jar")) continue;
                if (n.contains("sources") || n.contains("javadoc")) continue;
                int baseline = baselineFor(n);
                if (baseline > DEFAULT_BASELINE) {
                    System.out.println("  [SKIP] " + n + " 基线 JDK" + majorToJava(baseline)
                            + "，跳过 Java 8 链接加载（由字节码扫描覆盖）");
                    continue;
                }
                loadJarOnJava8(f);
            }
        }

        // 5. IoC 入口反射调用（Java 8 上真正跑起来）
        System.out.println("");
        System.out.println("--- IoC 入口在 Java 8 上反射调用 ---");
        try {
            Class<?> c = Class.forName("com.jvfault.core.bootstrap.JvfaultApplication");
            java.lang.reflect.Method m = c.getMethod("getVersion");
            Object r = m.invoke(null);
            ok("反射调用 getVersion() = " + r);
        } catch (Throwable t) {
            bad("IoC 入口反射调用失败", t);
        }

        // 汇总
        System.out.println("");
        System.out.println("========== 结果 ==========");
        System.out.println("pass = " + pass + ", fail = " + fail);
        if (!failures.isEmpty()) {
            System.out.println("");
            System.out.println("失败详情：");
            for (String s : failures) System.out.println("  - " + s);
        }
        System.out.println("==========================");
        if (fail > 0) System.exit(1);
    }

    /** 从 jar 名提取模块段并查基线 major；未知模块用默认 Java 8(52)。 */
    private static int baselineFor(String jarName) {
        Matcher m = JAR.matcher(jarName);
        if (m.matches()) {
            String module = m.group(1);
            Integer b = BASELINE.get(module);
            if (b != null) return b;
        }
        return DEFAULT_BASELINE;
    }

    private static int majorToJava(int major) {
        // 52→8, 53→9, ..., 61→17, ..., 65→21
        return major - 44;
    }

    /**
     * 扫描一个 jar 内所有 jvfault 类的字节码 major version。
     * 返回 [基类数, MR-JAR 多版本层类数]。
     * 基类的 major 必须 ≤ 该模块基线，否则记为失败（真正的基线违规）。
     */
    private static int[] scanJarBytecode(File jarFile) {
        JarFile jf = null;
        String jarName = jarFile.getName();
        int baseline = baselineFor(jarName);
        int base = 0;
        int mr = 0;
        List<String> badClasses = new ArrayList<String>();
        try {
            jf = new JarFile(jarFile);
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class")) continue;
                if (n.endsWith("module-info.class")) continue;
                int major = readMajorVersion(jf, e);
                if (major < 0) continue; // 读不到头，跳过

                boolean isMrLayer = n.startsWith("META-INF/versions/");
                if (isMrLayer) {
                    mr++;
                    // MR-JAR 高版本层允许 major > 基线，不视为违规
                    continue;
                }
                base++;
                if (major > baseline) {
                    String cls = n.substring(0, n.length() - 6).replace('/', '.');
                    badClasses.add(cls + " (major=" + major + " > 基线 " + baseline + ")");
                }
            }
            if (badClasses.isEmpty()) {
                ok(String.format("%-40s 基类 %3d (全部 <= %d=JDK%d) / MR层 %d",
                        jarName, base, baseline, majorToJava(baseline), mr));
            } else {
                bad(jarName + " 含 " + badClasses.size()
                        + " 个超出本模块基线的类(基线 major=" + baseline + ")", null);
                for (int i = 0; i < Math.min(badClasses.size(), 5); i++) {
                    System.out.println("       " + badClasses.get(i));
                }
            }
        } catch (Throwable t) {
            bad(jarName + " 无法打开/遍历", t);
        } finally {
            if (jf != null) try { jf.close(); } catch (Exception ignored) { }
        }
        return new int[]{ base, mr };
    }

    /** 读 class 文件头的 major version（偏移 6 处的 2 字节大端）。失败返回 -1。 */
    private static int readMajorVersion(JarFile jf, JarEntry e) {
        InputStream in = null;
        try {
            in = jf.getInputStream(e);
            byte[] hdr = new byte[8];
            int off = 0, n;
            while (off < 8 && (n = in.read(hdr, off, 8 - off)) != -1) off += n;
            if (off < 8) return -1;
            // hdr[0..3] = magic(CAFEBABE), hdr[4..5] = minor, hdr[6..7] = major
            return ((hdr[6] & 0xFF) << 8) | (hdr[7] & 0xFF);
        } catch (Throwable t) {
            return -1;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    /** 在 Java 8 上加载一个 jar 内的所有顶层类（不初始化，仅解析校验）。 */
    private static void loadJarOnJava8(File jarFile) {
        JarFile jf = null;
        int loaded = 0;
        int skipped = 0;
        List<String> errs = new ArrayList<String>();
        try {
            jf = new JarFile(jarFile);
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class")) continue;
                // MR-JAR 的 Java 9+ 层，Java 8 本来就该忽略
                if (n.startsWith("META-INF/versions/")) { skipped++; continue; }
                // module-info 属 Java 9+，Java 8 加载不了，跳过
                if (n.endsWith("module-info.class")) { skipped++; continue; }
                String cls = n.substring(0, n.length() - 6).replace('/', '.');
                // 跳过内部类（$ 符号），单独加载会失败
                if (cls.indexOf('$') >= 0) { skipped++; continue; }
                try {
                    Class.forName(cls, false, Java8RuntimeSmoke.class.getClassLoader());
                    loaded++;
                } catch (Throwable t) {
                    String msg = String.valueOf(t.getMessage());
                    // 只有 jvfault 自身类的 UnsupportedClassVersionError 才是基线违规
                    if (cls.startsWith("com.jvfault")
                            && (t instanceof UnsupportedClassVersionError
                                || msg.contains("class file has wrong version")
                                || msg.contains("Unsupported major.minor version"))) {
                        errs.add(cls + " -> " + t);
                    }
                    // 其他（三方依赖缺失等）不算基线问题，已由上一步字节码扫描兜底
                }
            }
            if (errs.isEmpty()) {
                if (loaded > 0) {
                    ok(String.format("%-40s 加载 %3d 类 (跳过 %d)", jarFile.getName(), loaded, skipped));
                }
                // loaded==0：该 jar 所有类都因三方依赖缺失而未链接，
                // 不在此处计失败（字节码扫描已覆盖）
            } else {
                bad(jarFile.getName() + " 存在基线违规类(" + errs.size() + " 个)", null);
                for (int i = 0; i < Math.min(errs.size(), 4); i++) {
                    System.out.println("       " + errs.get(i));
                }
            }
        } catch (Throwable t) {
            bad(jarFile.getName() + " 无法打开/遍历", t);
        } finally {
            if (jf != null) try { jf.close(); } catch (Exception ignored) { }
        }
    }

    private static boolean isJava8() {
        String cv = System.getProperty("java.class.version");
        try {
            return Double.parseDouble(cv) <= 52.0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void check(String name, boolean cond, String detail) {
        if (cond) ok(name); else bad(name + " — " + detail, null);
    }

    private static void ok(String msg) {
        pass++;
        System.out.println("  [OK]   " + msg);
    }

    private static void bad(String msg, Throwable t) {
        fail++;
        String detail = msg;
        if (t != null) {
            detail += " | " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        failures.add(detail);
        System.out.println("  [FAIL] " + detail);
    }
}
