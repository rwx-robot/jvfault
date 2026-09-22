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
    "tests"
).forEach { include(":$it") }

// runnable examples
    include(":examples:v0.5.0")
