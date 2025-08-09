package com.jvfault.aot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * aot 模块测试
 *
 * @since v0.9.0 (2023)
 */
@DisplayName("AOT 模块测试")
class AotModuleTest {

    static class SampleBean {
        public void doWork() {
        }
    }

    @Test
    @DisplayName("反射配置条目包含类名与开关")
    void testEntry() {
        Map<String, Object> entry = new ReflectConfigGenerator().entryFor(SampleBean.class);
        assertEquals(SampleBean.class.getName(), entry.get("name"));
        assertEquals(Boolean.TRUE, entry.get("allDeclaredConstructors"));
        assertEquals(Boolean.TRUE, entry.get("allPublicMethods"));
    }

    @Test
    @DisplayName("多 Bean JSON 输出")
    void testJson() {
        String json = new ReflectConfigGenerator().toJson(Collections.singletonList(SampleBean.class));
        assertTrue(json.startsWith("["));
        assertTrue(json.contains("com.jvfault.aot.AotModuleTest$SampleBean"));
        assertTrue(json.trim().endsWith("]"));
    }
}
