rootProject.name = "Trade-Buddy"

pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.kotlin.multiplatform.library") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.scijava.org/content/repositories/public/")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.scijava.org/content/repositories/public/")
    }
}

if (providers.gradleProperty("enableFoojayResolver").orNull == "true") {
    pluginManager.apply("org.gradle.toolchains.foojay-resolver-convention")
}

include(":composeApp")
include(":androidApp")
