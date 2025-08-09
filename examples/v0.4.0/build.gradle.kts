/**
 * v0.4.0 示例 — Web 控制器与路由
 * 运行: ./gradlew :examples:v0.4.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":web"))
    implementation(project(":platform-servlet"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v040.Application" }
