/**
 * jvfault-microservices - 传输抽象与消息模式
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    // 消息编解码
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
