/**
 * jvfault-transport-grpc - gRPC 传输适配器 (v0.6.0, 2020)
 * v1.0.2：真实集成（NettyServerBuilder/NettyChannelBuilder，本机回环）。
 * 不依赖 protobuf 代码生成：以 byte[] 作为请求/响应类型，复用 JSON wire。
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")

    implementation("io.grpc:grpc-api:1.62.2")
    implementation("io.grpc:grpc-stub:1.62.2")
    implementation("io.grpc:grpc-netty:1.62.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
