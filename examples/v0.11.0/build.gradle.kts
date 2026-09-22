/**
 * v0.11.0 示例 — RAG 检索增强（脚本模型，无需真实 LLM）
 * 运行: ./gradlew :examples:v0.11.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":ai"))
    implementation(project(":rag"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v110.Application" }
