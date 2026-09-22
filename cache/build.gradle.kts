/**
 * jvfault-cache - 缓存抽象与注解
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    // L2 缓存
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
