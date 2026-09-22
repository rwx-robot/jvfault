package com.jvfault.openapi;

import com.jvfault.core.annotation.Component;
import com.jvfault.web.WebApplication;
import com.jvfault.web.annotation.Controller;
import com.jvfault.web.annotation.Get;
import com.jvfault.web.annotation.Param;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-openapi 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("OpenAPI 模块测试")
class OpenApiModuleTest {

    @Component
    @Controller("/pets")
    static class PetController {

        @Get
        public String list() {
            return "[]";
        }

        @Get(":petId")
        public String detail(@Param("petId") long id) {
            return "pet";
        }
    }

    @Test
    @DisplayName("从路由表生成 OpenAPI 文档（路径参数转换）")
    void testBuild() {
        WebApplication app = new WebApplication(null);
        app.registerController(new PetController());
        OpenApiGenerator generator = new OpenApiGenerator(app)
                .title("Pet Store")
                .version("0.7.0");

        java.util.Map<String, Object> doc = generator.build();
        assertEquals("3.0.3", doc.get("openapi"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> info = (java.util.Map<String, Object>) doc.get("info");
        assertEquals("Pet Store", info.get("title"));
        assertEquals("0.7.0", info.get("version"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> paths = (java.util.Map<String, Object>) doc.get("paths");
        assertTrue(paths.containsKey("/pets"));
        assertTrue(paths.containsKey("/pets/{petId}"));
    }

    @Test
    @DisplayName("JSON 与 YAML 输出包含转换后的路径")
    void testOutputs() {
        WebApplication app = new WebApplication(null);
        app.registerController(new PetController());
        OpenApiGenerator generator = new OpenApiGenerator(app).title("Pet Store").version("1.0");

        String json = generator.toJson();
        assertTrue(json.contains("\"/pets\""));
        assertTrue(json.contains("\"/pets/{petId}\""), "路径参数应转为 {petId}");
        assertTrue(json.contains("PetController.detail"));

        String yaml = generator.toYaml();
        assertTrue(yaml.contains("openapi: 3.0.3"));
        assertTrue(yaml.contains("/pets/{petId}"));
    }
}
