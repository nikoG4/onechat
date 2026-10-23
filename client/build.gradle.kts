@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("com.android.application") version "8.5.2"
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
            implementation(compose.material)
            implementation(compose.ui)
            val adaptive = "0.1.0-alpha01"
            implementation("io.github.nikog4.adaptivekt:adaptive-core:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-components:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-layout:$adaptive")
            implementation("io.github.nikog4.adaptivekt:adaptive-feedback:$adaptive")
            val ktor = "3.2.3"
            implementation("io.ktor:ktor-client-core:$ktor")
            implementation("io.ktor:ktor-client-cio:$ktor")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
            implementation("io.ktor:ktor-client-websockets:$ktor")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.2.3")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }
        val desktopTest by getting {
            dependencies { implementation(kotlin("test-junit")) }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.10.1")
            }
        }
    }
}

android {
    namespace = "com.onechat.client"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.onechat.client"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-preview"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

compose.desktop {
    application {
        mainClass = "com.onechat.client.DesktopMainKt"
    }
}
