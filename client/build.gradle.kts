@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("com.android.library") version "8.5.2"
}

kotlin {
    androidTarget()
    jvm("desktop")
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            val adaptive = "0.1.0-alpha01"
            implementation("io.github.nikog4.adaptivekt:adaptive-core:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-components:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-layout:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-feedback:$adaptive")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
            dependencies { implementation(kotlin("test-junit")) }
        }
    }
}

android {
    namespace = "com.onechat.validation"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
