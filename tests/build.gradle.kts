/**
 * jvfault-tests - 跨模块端到端集成套件
 * 覆盖链路：web 路由 → platform-servlet 桥接 → 嵌入式 Jetty → security 守卫
 */

dependencies {
    testImplementation(project(":web"))
    testImplementation(project(":platform-servlet"))
    testImplementation(project(":security"))

    // 嵌入式容器（Jetty 11 = Jakarta EE 9 / Servlet 5.0）
    testImplementation("org.eclipse.jetty:jetty-server:11.0.24")
    testImplementation("org.eclipse.jetty:jetty-servlet:11.0.24")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

// 把框架版本与项目根路径注入测试运行时，避免 JpmsModulePathSmokeTest 把硬编码到 user.dir
val frameworkVersion = project.findProperty("jvfaultVersion") as String? ?: "0.0.0"
val projectRoot = rootProject.projectDir.absolutePath

// JpmsModulePathSmokeTest.verifyMrJarDescriptors 需要**所有**含 module-info.java
// 的模块 jar 已在 build/libs/ 就位。`:tests` 只声明了 web/platform-servlet/security
// 三个依赖，干净检出时其余 ~34 个 jar 尚未产出 → 测试失败（本地因 jar 已存在而看不出来）。
// 本模块是跨模块验收模块，依赖全量 jar 语义正确。
// 用「任务路径字符串」而非 Task 引用 —— 路径在执行图构建时惰性解析，
// 不依赖配置阶段的注册顺序。
val allModuleJarPaths: List<String> = rootProject.subprojects
    .filter { it != project }
    .map { "${it.path}:jar" }

tasks.withType<Test>().configureEach {
    systemProperty("jvfault.framework.version", frameworkVersion)
    systemProperty("jvfault.project.root", projectRoot)
    dependsOn(allModuleJarPaths)
}
