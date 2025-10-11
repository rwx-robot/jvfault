/**
 * v0.9.0 示例 — 插件 SPI + 编译期元数据 + AOT 反射注册
 * 运行: ./gradlew :examples:v0.9.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":plugin"))
    implementation(project(":aot"))
    // 编译期注解处理器：扫描 @Module 生成 META-INF/jvfault/modules.txt
    annotationProcessor(project(":apt"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v090.Application" }
