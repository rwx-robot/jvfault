package com.jvfault.apt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
