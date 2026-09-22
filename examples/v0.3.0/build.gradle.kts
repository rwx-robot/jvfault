/**
 * v0.3.0 示例 — 配置管理与 Profile
 * 运行: ./gradlew :examples:v0.3.0:run -Djvfault.profiles=prod
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v030.Application" }
