/**
 * 示例：Spring Boot 3 ↔ jvfault 双向桥接
 *
 * 演示两件事（都基于 spring-boot-starter）：
 *  - 正向（jvfault → Spring）：jvfault 的 @Component（Greeter）被暴露成 Spring bean，
 *    可被 @RestController 直接注入；
 *  - 反向（Spring → jvfault）：标注 @JvfaultComponent 的 Spring bean（ClockService）
 *    被注册进 jvfault 容器，jvfault 的 @Component 通过 @Inject 直接使用它。
 *
 * 运行: ./gradlew :examples:spring-boot-bridge:run
 *       然后访问 http://localhost:8080/greet?name=ny
 * 测试: ./gradlew :examples:spring-boot-bridge:test   （web=NONE 启动，不起 Tomcat）
 *
 * 注意：根构建对 :examples:* 跳过 java-library/maven-publish/jacoco，
 *       本模块自带 java/application 插件，Spring Boot 版本显式固定（与 starter 对齐 3.2.5）。
 */
plugins {
    id("java")
    id("application")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

application {
    mainClass = "com.jvfault.example.springbridge.ExampleApplication"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    // 根构建的 subprojects 块对 :examples:* 提前 return —— 示例**不继承**任何全局编译配置，
    // 必须自带。Spring MVC 的 @RequestParam 依赖反射参数名，缺 -parameters 会在
    // 真实请求时抛「Name for argument of type [java.lang.String] not specified」→ 500。
    // （该坑只在真实 HTTP 请求路径暴露，web=NONE 的上下文测试抓不到。）
    options.compilerArgs.add("-parameters")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
