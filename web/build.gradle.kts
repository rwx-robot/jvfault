/**
 * jvfault-web - 控制器、路由与请求管道（平台无关核心）
 */

dependencies {
    api(project(":core"))
    api(project(":aop"))
    api(project(":exception"))
    api(project(":validation"))

    api("org.slf4j:slf4j-api:2.0.13")

    // JSON 序列化
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
