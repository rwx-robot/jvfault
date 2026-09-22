/**
 * jvfault-test - 测试支持模块
 * 提供 JUnit 5 扩展，实现测试环境的容器装配与注入。
 */

dependencies {
    api(project(":core"))

    // JUnit 5 扩展 API
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")
    compileOnly("org.junit.platform:junit-platform-engine:1.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
