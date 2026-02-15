import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    androidLibrary {
        namespace = "de.tradebuddy.shared"
        compileSdk = 36
        minSdk = 26
        androidResources {
            enable = true
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.components.ui.tooling.preview)

                implementation(libs.navigation.compose)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.material.icons.core)
                implementation(libs.material.icons.extended)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.astronomy.engine)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }
    }
}

compose.desktop {
    application {
        mainClass = "de.tradebuddy.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradeBuddy"
            packageVersion = "1.0.0"
        }
    }
}
