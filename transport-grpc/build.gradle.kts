/**
 * jvfault-transport-grpc - gRPC 传输适配器 (v0.6.0, 2020)
 * 真实集成（InProcessServerBuilder / NettyServerBuilder）需在应用层接入，
 * 测试以配置 + lifecycle 单元测试为主。
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")

    implementation("io.grpc:grpc-api:1.62.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
