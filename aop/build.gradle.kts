/**
 * jvfault-aop - 切面编程与拦截器链
 */

dependencies {
    api(project(":core"))

    // 类代理（无接口目标）
    implementation("net.bytebuddy:byte-buddy:1.14.9")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
