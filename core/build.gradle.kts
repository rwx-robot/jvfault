/**
 * jvfault-core - IoC 容器与模块系统
 * JDK 8 兼容 (2015 基线)
 */

dependencies {
    // JSR-330 依赖注入规范
    api("jakarta.inject:jakarta.inject-api:2.0.1")

    // JSR-250 生命周期注解 (@PostConstruct, @PreDestroy)
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // 类路径扫描
    implementation("io.github.classgraph:classgraph:4.8.168")

    // 日志门面
    api("org.slf4j:slf4j-api:2.0.13")

    // ============ 测试依赖 ============
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
