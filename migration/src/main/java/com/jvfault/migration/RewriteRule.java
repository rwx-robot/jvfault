package com.jvfault.migration;

/**
 * 迁移重写规则 —— 注解/代码映射（Spring → jvfault）。
 * 对应 OpenRewrite: Recipe
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public record RewriteRule(
        String category,
        String from,
        String to,
        String note) {

    /** 内置规则表 */
    public static java.util.List<RewriteRule> defaults() {
        return java.util.List.of(
                new RewriteRule("annotation",
                        "org.springframework.stereotype.@Component",
                        "com.jvfault.core.annotation.@Component",
                        "语义一致"),
                new RewriteRule("annotation",
                        "org.springframework.beans.factory.annotation.@Autowired",
                        "com.jvfault.core.annotation.@Inject (或 jakarta.inject.@Inject)",
                        "JSR-330 标准化"),
                new RewriteRule("annotation",
                        "org.springframework.web.bind.annotation.@RestController",
                        "com.jvfault.web.annotation.@Controller",
                        "jvfault 控制器默认 JSON 渲染"),
                new RewriteRule("annotation",
                        "org.springframework.web.bind.annotation.@GetMapping",
                        "com.jvfault.web.annotation.@Get",
                        "路径占位符 {id} → :id"),
                new RewriteRule("annotation",
                        "org.springframework.boot.autoconfigure.@SpringBootApplication",
                        "com.jvfault.core.annotation.@Module (providers/imports 声明)",
                        "现代框架 风格模块化"),
                new RewriteRule("annotation",
                        "org.springframework.scheduling.annotation.@Scheduled",
                        "com.jvfault.scheduling.annotation.@Scheduled",
                        "属性名 fixedDelay → fixedDelayMillis"),
                new RewriteRule("expression",
                        "@Value(\"${key}\")",
                        "@Value(\"key\") 或 @Value(\"${key:default}\")",
                        "jvfault 支持裸 key"),
                new RewriteRule("dependency",
                        "spring-boot-starter-web",
                        "jvfault-web + jvfault-platform-servlet",
                        "Servlet 5.0+ 容器"));
    }
}
