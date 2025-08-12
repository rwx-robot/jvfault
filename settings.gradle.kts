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
    "tests"
).forEach { include(":$it") }

// runnable examples
    include(":examples:v0.3.0")
