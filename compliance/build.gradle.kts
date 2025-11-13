/**
 * jvfault-compliance - 审计日志与数据合规
 * 对应 roadmap v1.0.0 (2026)
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
