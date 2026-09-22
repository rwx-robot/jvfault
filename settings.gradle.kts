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
    "tests"
).forEach { include(":$it") }

// runnable examples
    include(":examples:v0.2.0")
