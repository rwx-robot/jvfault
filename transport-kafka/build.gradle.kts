/**
 * jvfault-transport-kafka - Kafka 传输（kafka-clients 真实实现）
 * 集成测试通过 docker CLI 管理 Kafka broker (v0.6.0, 2020)
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")
    api("org.apache.kafka:kafka-clients:3.6.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
