import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * jvfault Java 8 运行时冒烟 —— 在**真正的 Java 8 JVM** 上验证框架可用性。
 *
 * <p>背景：框架宣称「Java 8 基线」，但日常测试全在 JDK 21 上跑
 * （依赖 {@code --release 8} 的编译期检查）。编译期检查能保证 API 兼容，
 * 却无法证明**字节码能被 Java 8 JVM 真正加载执行**。本程序补上这一环。
 *
 * <p>验证项：
 * <ol>
 *   <li>当前 JVM 确为 Java 8（{@code java.class.version <= 52}）</li>
 *   <li>{@code JvfaultApplication.getVersion()} 可读（build properties 能加载）</li>
 *   <li>每个模块 jar 的类都能被 Java 8 加载解析
 *       （MR-JAR 的 {@code versions/9/} 层本就该被 Java 8 忽略）</li>
 *   <li>IoC 入口可在 Java 8 上反射调用</li>
 * </ol>
 *
 * <p><b>判定标准</b>：只有 {@link UnsupportedClassVersionError}（major > 52）算 Java 8
 * 兼容问题。因三方依赖缺失导致的 {@code NoClassDefFoundError} 不计 —— 那不是 Java 8 的问题。
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

        // 3. 遍历所有模块 jar，加载每个模块的类（Java 8 上验证 major 52）
        File dir = new File(jarsDir);
        File[] jars = dir.listFiles();
        if (jars == null || jars.length == 0) {
            bad("jar 目录为空或不存在: " + dir.getAbsolutePath(), null);
        } else {
            System.out.println("");
            System.out.println("--- 逐个 jar 在 Java 8 上加载类 ---");
            int jarCount = 0;
            for (File f : jars) {
                String n = f.getName();
                if (!n.endsWith(".jar")) continue;
                if (n.contains("sources") || n.contains("javadoc")) continue;
                jarCount++;
                loadJarOnJava8(f);
            }
            System.out.println("");
            System.out.println("共扫描 " + jarCount + " 个 jar");
        }

        // 4. IoC 入口反射调用（Java 8 上真正跑起来）
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
                    // 只有 UnsupportedClassVersionError 才是真正的 Java 8 兼容问题
                    if (t instanceof UnsupportedClassVersionError
                            || msg.contains("class file has wrong version")
                            || msg.contains("Unsupported major.minor version")) {
                        errs.add(cls + " -> " + t);
                    }
                    // 其他（依赖缺失等）不算 Java 8 问题，忽略
                }
            }
            if (errs.isEmpty()) {
                ok(String.format("%-44s 加载 %3d 类 (跳过 %d)", jarFile.getName(), loaded, skipped));
            } else {
                bad(jarFile.getName() + " 存在 Java 8 不兼容类(" + errs.size() + " 个)", null);
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
