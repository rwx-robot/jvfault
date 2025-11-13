/**
 * v0.1.0 示例 — 核心 IoC 容器与模块系统
 *
 * 运行:  ./gradlew :examples:v0.1.0:run
 */

plugins {
    id("java")
    id("application")
}

dependencies {
    implementation(project(":core"))
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(project(":test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

application {
    mainClass = "com.jvfault.example.v010.Application"
}
