/**
 * v0.2.0 示例 — AOP 切面与拦截器
 * 运行: ./gradlew :examples:v0.2.0:run
 */
plugins { id("java"); id("application") }

dependencies {
    implementation(project(":core"))
    implementation(project(":aop"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application { mainClass = "com.jvfault.example.v020.Application" }
