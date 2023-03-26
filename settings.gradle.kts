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
    "tests"
).forEach { include(":$it") }

// runnable examples
    include(":examples:v0.1.0")
