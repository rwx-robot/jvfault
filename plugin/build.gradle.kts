/**
 * jvfault-plugin - 插件 SPI 与生命周期
 * 对应 现代框架 动态模块思想的进程级扩展 (v0.9.0, 2023)
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
