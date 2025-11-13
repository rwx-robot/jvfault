/**
 * jvfault-transport-mqtt - MQTT (Eclipse Paho) 传输适配器 (v0.6.0, 2020)
 */

dependencies {
    api(project(":microservices"))

    api("org.slf4j:slf4j-api:2.0.13")

    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
