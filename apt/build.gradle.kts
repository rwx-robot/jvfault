/**
 * jvfault-apt - 编译期元数据生成 (注解处理器)
 * 对应 roadmap v0.9.0 (2023)
 */

dependencies {
    // 注解处理器只依赖 core 注解
    compileOnly(project(":core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.removeAll(listOf("-Xlint:all,-processing,-serial"))
}
