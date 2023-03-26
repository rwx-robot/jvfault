/**
 * v0.5.0 示例 — WebSocket 网关与 SSE
 * 运行: ./gradlew :examples:v0.5.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":websocket"))
    implementation(project(":sse"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v050.Application" }
