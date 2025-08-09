/**
 * jvfault-platform-reactive - Reactor 响应式处理器支持
 * 对应 现代框架 平台适配器思想 (v0.8.0, 2022)
 */

dependencies {
    api(project(":web"))

    api("org.slf4j:slf4j-api:2.0.13")
    api("io.projectreactor:reactor-core:3.6.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
