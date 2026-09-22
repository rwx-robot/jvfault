/**
 * jvfault-scheduling - 任务调度与 Cron
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
