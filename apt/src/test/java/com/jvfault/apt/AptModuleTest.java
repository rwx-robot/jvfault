package com.jvfault.apt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * apt 模块测试：处理器常量与资源路径约定。
 *
 * @since v0.9.0 (2023)
 */
@DisplayName("APT 模块测试")
class AptModuleTest {

    @Test
    @DisplayName("资源路径约定")
    void testResourcePath() {
        assertEquals("META-INF/jvfault/modules.txt", ModuleMetadataProcessor.RESOURCE);
    }

    @Test
    @DisplayName("处理器受支持注解配置正确")
    void testSupportedAnnotations() throws Exception {
        ModuleMetadataProcessor processor = new ModuleMetadataProcessor();
        assertTrue(processor.getSupportedAnnotationTypes()
                .contains("com.jvfault.core.annotation.Module"));
        assertEquals(javax.lang.model.SourceVersion.RELEASE_8, processor.getSupportedSourceVersion());
    }

    @Test
    @DisplayName("处理器已在 META-INF/services 注册（消费者挂 annotationProcessor 即可生效）")
    void processorIsRegisteredAsService() throws Exception {
        String serviceFile = "META-INF/services/javax.annotation.processing.Processor";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(serviceFile)) {
            assertNotNull(in, "缺少 " + serviceFile + "，注解处理器不会被 javac 发现");
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                content.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
            }
            assertTrue(content.toString().contains(ModuleMetadataProcessor.class.getName()),
                    content.toString());
        }
    }

    @Test
    @DisplayName("真实编译：@Module 类生成 META-INF/jvfault/modules.txt")
    void generatesModuleMetadataOnRealCompile() throws Exception {
        Path work = Files.createTempDirectory("jvfault-apt");
        Path source = work.resolve("DemoModule.java");
        Files.write(source, ("package demo;\n"
                + "import com.jvfault.core.annotation.Module;\n"
                + "@Module\n"
                + "public class DemoModule {\n}\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Path output = Files.createDirectories(work.resolve("out"));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "需要 JDK（而非 JRE）运行测试");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(source.toFile());
            List<String> options = Arrays.asList("-d", output.toString(),
                    "-classpath", System.getProperty("java.class.path"));
            StringWriter errors = new StringWriter();
            boolean success = compiler.getTask(errors, fileManager, null, options, null, units).call();
            assertTrue(success, "编译失败: " + errors);

            Path resource = output.resolve(ModuleMetadataProcessor.RESOURCE);
            assertTrue(Files.exists(resource), "未生成 " + ModuleMetadataProcessor.RESOURCE);
            assertEquals("demo.DemoModule",
                    new String(Files.readAllBytes(resource), java.nio.charset.StandardCharsets.UTF_8).trim());
        }
    }
}
