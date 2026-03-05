/**
 * jvfault-spring-boot-starter - Spring Boot 3 自动配置桥接（**可选插件**）
 *
 * 设计原则（Ny 2026-09-24：「可引入，但注意模块化、插件化」）：
 *  - **依赖方向单向**：本模块 -> core。核心（core）绝不依赖本模块，也不依赖 Spring，
 *    「零 Spring」承诺不受影响。
 *  - Spring 依赖声明为 `compileOnly`：**不会**把 Spring 传递进使用者的依赖树。
 *    消费者自带 Spring Boot；不引入本 starter 就完全没有 Spring。
 *  - 编译目标 **JDK 21**（当前 JDK；Spring Boot 3 要求 17+）。
 *  - 不提供 `module-info.java`：Spring Boot 的自动配置并非 JPMS 友好，
 *    本模块按 classpath 适配器发布（与 `tests` 一样不进 MR-JAR）。
 *
 * 注意：Gradle 的 `compileOnly` **不会**进入测试的编译/运行类路径，
 * 所以测试用的 Spring 依赖必须在下面 `testImplementation` 里再声明一遍。
 */
dependencies {
    api(project(":core"))

    // Spring Boot 3：仅编译期需要，不强制传递给使用者
    compileOnly("org.springframework.boot:spring-boot:3.2.5")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.5")

    // 测试需要真实加载 Spring（compileOnly 不进 test classpath）
    testImplementation("org.springframework.boot:spring-boot:3.2.5")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure:3.2.5")
    testImplementation("org.springframework.boot:spring-boot-test:3.2.5")
    testImplementation("org.springframework:spring-test:6.1.6")
    // ApplicationContextRunner 的 AssertableApplicationContext 继承 AssertProvider，
    // 编译期必须能拿到 AssertJ（spring-boot-test 只是可选依赖，不会自动带进来）
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
