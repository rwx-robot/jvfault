/**
 * jvfault-config - 配置管理与环境抽象
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")

    // YAML 配置源
    implementation("org.yaml:snakeyaml:2.2")

    // JSON 配置源
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
