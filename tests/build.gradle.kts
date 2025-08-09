/**
 * jvfault-tests - 跨模块端到端集成套件
 * 覆盖链路：web 路由 → platform-servlet 桥接 → 嵌入式 Jetty → security 守卫
 */

dependencies {
    testImplementation(project(":web"))
    testImplementation(project(":platform-servlet"))
    testImplementation(project(":security"))

    // 嵌入式容器（Jetty 11 = Jakarta EE 9 / Servlet 5.0）
    testImplementation("org.eclipse.jetty:jetty-server:11.0.24")
    testImplementation("org.eclipse.jetty:jetty-servlet:11.0.24")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
