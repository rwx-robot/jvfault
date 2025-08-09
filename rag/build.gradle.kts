/**
 * jvfault-rag - 检索增强生成 (RAG)
 * 对应 roadmap v0.11.0 (2025)
 *
 * AI 时代模块: 编译目标 JDK 17
 */

dependencies {
    api(project(":ai"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
