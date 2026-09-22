/**
 * v0.10.0 示例 — 结构化日志 + Native 提示
 * 运行: ./gradlew :examples:v0.10.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":logging"))
    implementation(project(":native"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v1000.Application" }
