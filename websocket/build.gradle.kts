/**
 * jvfault-websocket - WebSocket 网关 (声明式消息处理)
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    // 消息编解码（JSON 事件载荷）
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
