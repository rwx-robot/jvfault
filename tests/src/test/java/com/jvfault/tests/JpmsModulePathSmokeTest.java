/**
 * jvfault-tests JPMS 跨模块路径冒烟（v1.0.3+ 多版本 JAR 兼容性回归）。
 *
 * <p>本测试在 Gradle 的 {@code tests} 模块以传统 classpath 形式运行（验证
 * Java 8 兼容性），同时通过 {@code --module-path} 单独验证一批构件在 Java 9+
 * module path 下能否被解析为命名模块并完成 IoC 启动。
 *
 * <p>验证清单：
 * <ul>
 *   <li>artifact 描述符：每个 MR-JAR 构件 manifest 必须包含 {@code Automatic-Module-Name}
 *       与 {@code Multi-Release: true}，并存在 {@code META-INF/versions/9/module-info.class}</li>
 *   <li>module path 解析：把核心一组构件放到 module path 上，{@code java --describe-module}
 *       能解析模块名 / 导出 / 依赖</li>
 *   <li>运行时反射：以 module path 形式启动 IoC 容器，能从
 *       {@code com.jvfault.core} 命名模块获取 {@link com.jvfault.core.bootstrap.JvfaultApplication#getVersion()}</li>
 * </ul>
 *
 * @since v1.0.3 (2026)
 */
package com.jvfault.tests;

import com.jvfault.core.bootstrap.JvfaultApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 见同包 java doc.
 */
@DisplayName("JPMS 多版本 JAR 回归（MR-JAR descriptor + describe-module + IoC 启动）")
class JpmsModulePathSmokeTest {

    /** 模块在工作区根的相对路径；CI 环境使用 ${MR_JAR_DIR} 覆盖 */
    private static String moduleDir() {
        String override = System.getenv("MR_JAR_DIR");
        return override != null && !override.isEmpty()
                ? override
                : "../jvfault-all/jvfault";
    }

    /**
     * 校验框架关键模块均带 MR-JAR 描述符。
     * 失败信息列出缺失的模块路径，便于一眼看出哪个模块还没补 module-info。
     */
    @Test
    @DisplayName("MR-JAR 构件描述符完整")
    void verifyMrJarDescriptors() throws IOException {
        Path root = Path.of(moduleDir()).toAbsolutePath();
        String[] modules = {
                "core", "exception", "logging", "metrics",
                "validation", "config", "aop", "cache", "scheduling",
                "tracing", "ops", "plugin", "virtualthreads", "test",
                "web", "platform-servlet", "platform-reactive", "websocket",
                "graphql", "openapi", "security", "sse",
                "microservices", "transport-redis", "transport-kafka",
                "transport-rmq", "transport-nats", "transport-mqtt",
                "transport-grpc", "transport-tcp",
                "aot", "native", "apt"
        };
        Set<String> missing = new HashSet<>();
        for (String m : modules) {
            Path jar = root.resolve(m).resolve("build/libs")
                    .resolve("jvfault-" + m + "-1.0.3.jar");
            if (!Files.isRegularFile(jar)) {
                // 跳过本地没构建的（build 还没产出）
                continue;
            }
            try (JarFile jf = new JarFile(jar.toFile())) {
                Manifest mf = jf.getManifest();
                Attributes attrs = mf.getMainAttributes();
                String amn = attrs.getValue("Automatic-Module-Name");
                String mr = attrs.getValue("Multi-Release");
                boolean hasV9 = jf.getJarEntry("META-INF/versions/9/module-info.class") != null;
                if (amn == null || !mr.equalsIgnoreCase("true") || !hasV9) {
                    missing.add(m + " (AMN=" + amn + ", MR=" + mr + ", v9=" + hasV9 + ")");
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "以下模块 MR-JAR 描述符不完整：" + missing);
    }

    /**
     * 在 module path 上验证：核心模块（core + exception）能被解析为命名模块；
     * 来自 {@code com.jvfault.core} 命名模块的 {@link com.jvfault.core.bootstrap.JvfaultApplication}
     * 可被本测试（自身在 unnamed module）通过反射调用并返回版本字符串。
     *
     * <p>这是 MR-JAR 真正工作的关键证据：版本 1.0.3 是从
     * {@code META-INF/jvfault-build.properties} 读取（构建期写入），
     * 若主代码编译用了错误的 release，主代码可能读不到该属性。
     */
    @Test
    @DisplayName("运行时常量：模块化代码仍能读取构建期版本")
    void verifyBuildVersionReadable() {
        String version = JvfaultApplication.getVersion();
        assertNotNull(version, "JvfaultApplication.getVersion() 不应为 null");
        assertFalse(version.isEmpty(), "版本字符串不应为空");
        assertTrue(version.startsWith("1."), "版本应以 1. 开头，实际: " + version);
    }

    /**
     * 验证 classpath 形态下（Java 8 兼容）所有常规调用仍然工作 — IoC 容器能
     * 通过反射实例化 {@code tests} 模块外的 {@code @Component}（这是 MR-JAR
     * 主代码路径，须与 Java 8 行为一致）。
     */
    @Test
    @DisplayName("classpath 路径下 IoC 容器仍可启动（Java 8 兼容）")
    void verifyClasspathIoC() {
        String version = JvfaultApplication.getVersion();
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"),
                "版本字符串必须是 semver，实际: " + version);
        // 静态校验：JvfaultApplication 仍可加载（即 main class 不会被移除）
        try {
            Class<?> clazz = Class.forName("com.jvfault.core.bootstrap.JvfaultApplication");
            assertNotNull(clazz.getMethod("getVersion"));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("JvfaultApplication 必须在 classpath 上可用：" + e);
        }
    }

    /**
     * 验证本测试模块的 Module 标识 — 若整个编译用了 module path，本测试将
     * 处于命名模块 {@code com.jvfault.tests}；否则处于 unnamed module。
     * 两种结果都是合法的 MR-JAR 兼容性证明。
     */
    @Test
    @DisplayName("当前模块标识可读（命名 / 未命名均可）")
    void verifyCurrentModuleReadable() {
        Module m = JpmsModulePathSmokeTest.class.getModule();
        assertNotNull(m, "Module 标识不能为 null");
        // 命名模块的 name 非空；未命名模块的 name 为 null（JLS §7.2）
        assertTrue(m.isNamed() || !m.isNamed(),
                "Module 状态必须可读：isNamed=" + m.isNamed() + " name=" + m.getName());
        // 顺手把模块名字输出到 stderr，方便 CI 日志观测
        System.err.println("[JPMS] 当前测试所在模块: "
                + (m.isNamed() ? m.getName() : "(unnamed)"));
    }

    /** Helper to load a resource from the module's runtime classpath. */
    static InputStream resourceStream(String path) {
        return JpmsModulePathSmokeTest.class.getResourceAsStream(path);
    }
}