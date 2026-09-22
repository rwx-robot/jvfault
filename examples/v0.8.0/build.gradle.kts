/**
 * v0.8.0 示例 — 虚拟线程高并发演示
 * 运行: ./gradlew :examples:v0.8.0:run
 */

plugins {
    id("java")
    id("application")
}

dependencies {
    implementation(project(":virtualthreads"))
}

application {
    mainClass = "com.jvfault.example.v080.Application"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
