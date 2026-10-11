pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "8.13.2"
        id("org.jetbrains.kotlin.android") version "2.3.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
        id("app.cash.paparazzi") version "2.0.0-alpha05"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rem-compose-design-system"
include(":RemDesignSystem")
project(":RemDesignSystem").projectDir = file("RemDesignSystem")
