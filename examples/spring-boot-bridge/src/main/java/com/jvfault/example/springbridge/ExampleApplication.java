package com.jvfault.example.springbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot ↔ jvfault 双向桥接示例入口。
 *
 * <p>配置见 {@code src/main/resources/application.properties}：
 * <ul>
 *   <li>{@code jvfault.base-packages} —— 激活 jvfault 自动配置（必填，opt-in）；</li>
 *   <li>{@code jvfault.import-spring-beans=true} —— 开启反向注入（Spring → jvfault）。</li>
 * </ul>
 *
 * <p>运行：{@code ./gradlew :examples:spring-boot-bridge:run}，
 * 然后访问 {@code http://localhost:8080/greet?name=ny}。
 *
 * @since v1.0.14 (2026)
 */
@SpringBootApplication
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
