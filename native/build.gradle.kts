/**
 * jvfault-native - GraalVM 原生镜像支持
 * 对应 roadmap v0.10.0 (2024)
 * JPMS：项目名 'native' 是 Java 保留字，故模块名取 com.jvfault.nativeimage（见根 build.gradle.kts）
 */

dependencies {
    api(project(":core"))
    api(project(":aot"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
