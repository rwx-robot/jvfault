/**
 * jvfault-transport-rmq - RabbitMQ 传输适配器 (v0.6.0, 2020)
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")

    implementation("com.rabbitmq:amqp-client:5.20.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
