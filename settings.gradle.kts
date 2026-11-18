pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // Plugin versions live here so each module loads only what it needs
    // (the core module must build without the Android SDK or Google's Maven).
    plugins {
        id("com.android.application") version "8.7.3"
        kotlin("android") version "2.1.0"
        kotlin("jvm") version "2.1.0"
        kotlin("plugin.serialization") version "2.1.0"
        kotlin("plugin.compose") version "2.1.0"
    }
}
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "cs2-lineups"

// Pure Kotlin lineup logic, shared with the future desktop companion.
include(":core")
// Android app. Skip it with -PcoreOnly on machines without the Android SDK.
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}
