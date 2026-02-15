import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
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
                implementation(libs.kotlinx.datetime)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("src/jvmWasmMain/kotlin")
            dependencies {
                implementation(libs.astronomy.engine)
            }
        }

        val nativeMain = maybeCreate("nativeMain").apply {
            dependsOn(commonMain)
        }
        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(nativeMain)
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }

        val wasmJsMain by getting {
            dependsOn(commonMain)
            kotlin.srcDir("src/jvmWasmMain/kotlin")
            dependencies {
                implementation(npm("astronomy-engine", "2.1.19"))
            }
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
