/**
 * jvfault-ai - LLM 客户端抽象 (AI 原生)
 * 对应 roadmap v0.11.0 (2025), Spring AI 风格
 *
 * AI 时代模块: 编译目标 JDK 17
 */

dependencies {
    api(project(":core"))
    api(project(":web"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
