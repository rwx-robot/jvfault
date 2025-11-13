/**
 * jvfault-security - JWT、口令哈希与 Web 守卫
 */

dependencies {
    api(project(":core"))
    api(project(":web"))

    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
