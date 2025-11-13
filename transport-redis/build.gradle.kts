/**
 * jvfault-transport-redis - Redis Pub/Sub 传输 (Lettuce 真实实现)
 * 集成测试通过 docker CLI 管理真实 Redis 容器 (v0.6.0, 2020)
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")
    api("io.lettuce:lettuce-core:6.3.0.RELEASE")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
