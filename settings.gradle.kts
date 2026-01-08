pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "jvfault"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

listOf(
    "core", "aop", "config", "logging", "validation", "exception",
    "scheduling", "cache", "tracing", "metrics", "plugin", "apt",
    "microservices", "test", "tests",
    "web", "platform-servlet", "platform-reactive", "websocket", "sse",
    "openapi", "graphql", "security",
    "transport-tcp", "transport-grpc", "transport-kafka", "transport-redis",
    "transport-nats", "transport-rmq", "transport-mqtt",
    "aot", "native", "virtualthreads",
    "ai", "rag", "mcp",
    "compliance", "migration", "ops",
    "spring-boot-starter"
).forEach { include(":$it") }

listOf(
    "v0.8.0", "v0.9.0", "v0.10.0", "v0.11.0", "v1.0.0"
).forEach { include(":examples:$it") }
