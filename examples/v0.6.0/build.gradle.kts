/**
 * v0.6.0 示例 — 微服务传输（进程内 + TCP）
 * 运行: ./gradlew :examples:v0.6.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":microservices"))
    implementation(project(":transport-tcp"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v060.Application" }
