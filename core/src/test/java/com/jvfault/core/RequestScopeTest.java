package com.jvfault.core;

import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import com.jvfault.core.scopefixture.RequestBean;
import com.jvfault.core.scopefixture.ScopeModule;
import com.jvfault.core.scopefixture.ScopeSingletonBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQUEST 作用域回归（#10 修复后）。
 *
 * <p>验证：每个请求作用域内同一实例、跨请求换新实例、退域触发 @PreDestroy、
 * 嵌套作用域互相隔离，且 SINGLETON 不受 REQUEST 改动影响。
 */
@DisplayName("Core REQUEST 作用域")
class RequestScopeTest {

    private ModuleContainer container;

    @BeforeEach
    void setUp() {
        container = JvfaultApplication.createContainer(ScopeModule.class);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.destroy();
        }
    }

    @Test
    @DisplayName("无活动请求作用域时解析 REQUEST bean 应 fail-fast")
    void requestScopeRequiresActiveScope() {
        BeanRegistry registry = container.getBeanRegistry();
        assertThrows(IllegalStateException.class,
                () -> registry.getBean(RequestBean.class),
                "REQUEST bean 在无活动请求作用域时被解析应直接失败，而非退化成单例串号");
    }

    @Test
    @DisplayName("同一请求作用域内同实例；跨请求换新实例；退域触发 @PreDestroy")
    void requestScopeIsolatedAndDestroyedOnExit() {
        BeanRegistry registry = container.getBeanRegistry();

        Object s1 = registry.enterRequestScope();
        RequestBean a = registry.getBean(RequestBean.class);
        RequestBean a2 = registry.getBean(RequestBean.class);
        assertSame(a, a2, "同一请求内应返回同一实例");
        a.mark = 7;
        assertTrue(a2.mark == 7, "同一请求内状态应共享");
        RequestBean captured = a;
        registry.exitRequestScope(s1);
        assertTrue(captured.destroyed, "退域应触发 @PreDestroy");

        // 新请求：全新实例，不串号
        Object s2 = registry.enterRequestScope();
        RequestBean b = registry.getBean(RequestBean.class);
        assertNotSame(a, b, "跨请求应换新实例");
        assertTrue(b.mark == 0, "新请求实例状态应为初始值");
        registry.exitRequestScope(s2);
    }

    @Test
    @DisplayName("嵌套请求作用域各自隔离，退出内层后外层实例保持一致")
    void nestedRequestScopesAreIndependent() {
        BeanRegistry registry = container.getBeanRegistry();
        Object outer = registry.enterRequestScope();
        RequestBean o = registry.getBean(RequestBean.class);
        Object inner = registry.enterRequestScope();
        RequestBean i = registry.getBean(RequestBean.class);
        assertNotSame(o, i, "嵌套内层应是新实例");
        i.mark = 5;
        assertTrue(o.mark == 0, "内外层状态隔离");
        registry.exitRequestScope(inner);
        // 退出内层后，外层仍可用且是原来的实例
        RequestBean o2 = registry.getBean(RequestBean.class);
        assertSame(o, o2, "退出内层后外层实例应保持一致");
        registry.exitRequestScope(outer);
    }

    @Test
    @DisplayName("SINGLETON 不受 REQUEST 改动影响，仍是全局单例")
    void singletonUnaffected() {
        BeanRegistry registry = container.getBeanRegistry();
        ScopeSingletonBean s1 = registry.getBean(ScopeSingletonBean.class);
        ScopeSingletonBean s2 = registry.getBean(ScopeSingletonBean.class);
        assertSame(s1, s2);
    }
}
