package com.jvfault.nativeimage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * native 模块测试
 *
 * @since v0.10.0 (2024)
 */
@DisplayName("Native 模块测试")
class NativeModuleTest {

    @Test
    @DisplayName("注册去重与聚合输出")
    void testRegisterAndGenerate() {
        NativeRuntimeHints hints = new NativeRuntimeHints()
                .register(String.class, String.class, Integer.class);
        assertEquals(2, hints.getReflectionClasses().size());
        String json = hints.generateReflectConfig();
        assertTrue(json.contains("java.lang.String"));
        assertTrue(json.contains("java.lang.Integer"));
    }

    @Test
    @DisplayName("native 探测默认关闭")
    void testNativeDetection() {
        assertFalse(NativeRuntimeHints.isNative());
    }
}
