/**
 * jvfault-tracing - 分布式追踪与可观测性 (roadmap v0.7.0, 2021)
 * JDK 8 兼容
 */

dependencies {
    // 核心容器（注解、IoC 与扩展点）
    api(project(":core"))

    // 日志门面（LoggingSpanExporter 使用）
    api("org.slf4j:slf4j-api:2.0.13")

    // ============ 测试依赖 ============
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
