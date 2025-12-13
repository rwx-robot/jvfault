package com.jvfault.core.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 框架版本解析测试 —— 防止版本号在源码中硬编码后随迭代漂移。
 *
 * @since v1.0.1 (2026)
 */
@DisplayName("框架版本解析")
class JvfaultApplicationVersionTest {

    @Test
    @DisplayName("版本取自构建产物，不回退 unknown")
    void versionComesFromBuildInfo() {
        String version = JvfaultApplication.getVersion();
        assertNotEquals("unknown", version, "未解析到构建版本（META-INF/jvfault-build.properties 缺失？）");
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"), "版本号格式异常: " + version);
    }
}
