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
    plugins {
        id("com.android.application") version "8.3.0"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.22"
        id("com.google.dagger.hilt.android") version "2.51.1"
        id("com.google.devtools.ksp") version "1.9.22-1.0.17"
        kotlin("kapt") version "1.9.22"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Add if you need any JitPack dependencies
    }
}

rootProject.name = "MindScribe"
include(":app")