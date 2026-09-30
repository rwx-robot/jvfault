package com.jvfault.core;

import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * createContainer 健壮性（#12）：null 参数不应 NPE，应明确拒绝或优雅回退。
 */
@DisplayName("Core createContainer 健壮性")
class CreateContainerNullArgsTest {

    @Test
    @DisplayName("createContainer(X.class, (String[]) null) 不应 NPE")
    void nullBasePackagesVarargsDoesNotNpe() {
        // 旧实现会在 basePackages.length 处 NPE；修复后回退到默认包扫描。
        ModuleContainer c = JvfaultApplication.createContainer(CoreTestModule.class, (String[]) null);
        assertNotNull(c);
        assertNotNull(c.getBeanRegistry());
        c.destroy();
    }

    @Test
    @DisplayName("createContainer(null) 应明确拒绝而非 NPE")
    void nullRootModuleClassRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JvfaultApplication.createContainer(null));
        assertNotNull(ex.getMessage());
    }
}
