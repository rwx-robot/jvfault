package com.jvfault.example.v090;

import com.jvfault.aot.ReflectConfigGenerator;
import com.jvfault.plugin.PluginManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * v0.9.0 示例入口：插件生命周期（依赖拓扑）、编译期模块元数据、AOT 反射注册。
 *
 * @since v0.9.0 (2023)
 */
public class Application {

    private static final String MODULE_METADATA = "META-INF/jvfault/modules.txt";

    public static void main(String[] args) throws Exception {
        System.out.println("== jvfault v0.9.0: 插件体系 / 编译期处理 / AOT ==");

        // 1) 编译期注解处理器产物：零反射发现模块
        List<String> modules = readModuleMetadata();
        System.out.println("  [apt] " + MODULE_METADATA + " -> " + modules);

        // 2) 插件：ServiceLoader 发现 + 依赖拓扑排序 + 生命周期
        Map<String, String> shared = new HashMap<>();
        shared.put("env", "demo");
        PluginManager manager = new PluginManager(shared, false);
        int loaded = manager.loadFromClasspath();
        List<String> order = manager.topologicalOrder();
        System.out.println("  [plugin] 发现 " + loaded + " 个插件，启动顺序 " + order);
        if (!order.equals(Arrays.asList("metrics", "reports"))) {
            throw new IllegalStateException("依赖拓扑顺序错误: " + order);
        }
        manager.startAll();
        for (PluginManager.Loaded each : manager.getAll()) {
            System.out.println("    - " + each.descriptor.getId()
                    + " v" + each.descriptor.getVersion() + " -> " + each.state);
        }
        manager.stopAll();
        for (PluginManager.Loaded each : manager.getAll()) {
            System.out.println("    - " + each.descriptor.getId() + " -> " + each.state);
        }

        // 3) AOT：为容器 Bean 生成 native-image 反射注册条目
        ReflectConfigGenerator generator = new ReflectConfigGenerator();
        System.out.println("  [aot] reflect-config.json:");
        System.out.println(generator.toJson(Arrays.asList(AppModule.class, GreetingService.class)));
        System.out.println("== 完成 ==");
    }

    private static List<String> readModuleMetadata() throws Exception {
        List<String> lines = new ArrayList<>();
        ClassLoader loader = Application.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(MODULE_METADATA)) {
            if (in == null) {
                return lines;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line.trim());
                    }
                }
            }
        }
        return lines;
    }
}
