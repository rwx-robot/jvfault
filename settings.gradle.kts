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
    "core",
    "test",
    "aop",
    "exception",
    "validation",
    "web",
    "config",
    "platform-servlet",
    "websocket",
    "sse",
    "microservices",
    "transport-tcp",
    "transport-grpc",
    "transport-kafka",
    "transport-redis",
    "transport-nats",
    "transport-rmq",
    "transport-mqtt",
    "openapi",
    "graphql",
    "scheduling",
    "cache",
    "tracing",
    "metrics",
    "virtualthreads",
    "plugin",
    "apt",
    "aot",
    "native",
    "logging",
    "ai",
    "rag",
    "mcp",
    "tests"
).forEach { include(":$it") }

// runnable examples
    include(":examples:v0.11.0")
