/**
 * jvfault-platform-servlet - Servlet 桥接适配器
 *
 * 编译期仅依赖 Servlet 5.0 API（Java 8 基线）；
 * 嵌入式容器（Jetty 11+）由应用运行时提供。
 */

dependencies {
    api(project(":web"))

    // Jakarta EE 9 Servlet API（Java 8 兼容的最后主线）
    compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
