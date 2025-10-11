/**
 * v1.0.0 示例 — 安全 / 合规 / 迁移 / 运维（生产就绪专题）
 * 运行: ./gradlew :examples:v1.0.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":security"))
    implementation(project(":compliance"))
    implementation(project(":migration"))
    implementation(project(":ops"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v100.Application" }
