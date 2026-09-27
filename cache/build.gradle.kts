/**
 * jvfault-cache - 缓存抽象与注解
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    // L2 缓存（3.x 与 Spring Boot 3 同款；要求 Java 11+，本框架 Java 17 基线满足）
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
