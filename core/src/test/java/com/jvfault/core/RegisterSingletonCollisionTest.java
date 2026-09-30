package com.jvfault.core;

import com.jvfault.core.container.DefaultBeanRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * registerSingleton 纵深防御（#13）。
 */
@DisplayName("Core registerSingleton 纵深防御")
class RegisterSingletonCollisionTest {

    @Test
    @DisplayName("同名且不同实例重复注册应抛 IllegalStateException")
    void duplicateWithDifferentInstanceThrows() {
        DefaultBeanRegistry reg = new DefaultBeanRegistry();
        String first = "first";
        reg.registerSingleton("x", first);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reg.registerSingleton("x", "second"));
        assertTrue(ex.getMessage().contains("x"), "异常信息应指出冲突的 bean 名");
        // 原实例不受影响，拒绝静默覆盖
        assertSame(first, reg.getBean("x"));
    }

    @Test
    @DisplayName("同名且同一实例重复注册应幂等（不抛）")
    void duplicateWithSameInstanceIsIdempotent() {
        DefaultBeanRegistry reg = new DefaultBeanRegistry();
        Object o = new Object();
        reg.registerSingleton("x", o);
        reg.registerSingleton("x", o);
        assertSame(o, reg.getBean("x"));
    }
}
