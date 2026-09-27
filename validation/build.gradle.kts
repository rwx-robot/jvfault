/**
 * jvfault-validation - Bean 校验引擎
 *
 * 委托主流 SDK：jakarta.validation API + Hibernate Validator 引擎
 * （与 Spring Boot 3 / Quarkus / Jakarta EE 同一套校验栈）。
 */

dependencies {
    api(project(":core"))

    api("org.slf4j:slf4j-api:2.0.13")
    // 标准校验 API（Jakarta EE 10）
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    // 校验引擎实现（主流框架默认 Provider）
    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    // 消息插值所需的 Jakarta EL 实现（运行时）
    runtimeOnly("org.glassfish.expressly:expressly:5.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
