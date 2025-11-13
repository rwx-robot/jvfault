/**
 * v0.7.0 示例 — OpenAPI + 调度 + 缓存
 * 运行: ./gradlew :examples:v0.7.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":web"))
    implementation(project(":openapi"))
    implementation(project(":scheduling"))
    implementation(project(":cache"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v070.Application" }
