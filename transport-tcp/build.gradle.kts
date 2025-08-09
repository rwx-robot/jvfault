/**
 * jvfault-transport-tcp - TCP 传输（JDK NIO/Socket，零额外依赖）
 */

dependencies {
    api(project(":microservices"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
