/**
 * jvfault-graphql - GraphQL 端点
 */

dependencies {
    api(project(":web"))

    api("org.slf4j:slf4j-api:2.0.13")
    api("com.graphql-java:graphql-java:21.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}
