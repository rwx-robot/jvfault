/**
 * jvfault-virtualthreads - 虚拟线程执行器与结构化并发
 * 对应 roadmap v0.8.0 (2022, JDK 21)
 *
 * 需要 JDK 21（release 21），为叶子模块（无其他框架模块依赖它）
 */

dependencies {
    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
