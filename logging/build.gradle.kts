/**
 * jvfault-logging - 结构化日志 (roadmap v0.10.0, 2024)
 * JDK 8 兼容
 */

dependencies {
    // 核心容器（注解、IoC 与扩展点）
    api(project(":core"))

    // 日志门面
    api("org.slf4j:slf4j-api:2.0.13")

    // ============ 测试依赖 ============
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
