import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.googleServices)
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }

    jvm("desktop")


//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        moduleName = "composeApp"
//        browser {
//            val rootDirPath = project.rootDir.path
//            val projectDirPath = project.projectDir.path
//            commonWebpackConfig {
//                outputFileName = "composeApp.js"
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(rootDirPath)
//                        add(projectDirPath)
//                    }
//                }
//            }
//        }
//        binaries.executable()
//    }

    sourceSets {
//        val iosArm64Main by getting{
//            dependencies {
//                configurations.all {
//                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
//                }
//            }
//        }


        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.ui.android)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.compose.emoji.picker)
            implementation(libs.androidx.ui.tooling.preview.android)
            implementation(libs.sqldelight.android-driver)
        }


        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.ui.tooling)
            implementation(libs.coil)
            implementation(libs.coil.network.okhttp)
            implementation(libs.coil3.coil.compose)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.cmp.image.pick.n.crop)
            implementation(libs.gson)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            
            // Firebase KMP
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.common)
            
            // SQLDelight
            implementation(libs.sqldelight.coroutines-extensions)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content-negotiation)
            implementation(libs.ktor.serialization.kotlinx-json)
            implementation(libs.compose.country-code.picker)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite-driver)
        }


//        val wasmJsMain by getting {
//            dependencies {
//                configurations.all {
//                    exclude(group = "io.coil-kt", module = "coil")
//                    exclude(group = "io.coil-kt", module = "coil-base")
//                    exclude(group = "io.coil-kt", module = "coil-compose")
//                    exclude(group = "io.coil-kt", module = "coil-gif")
//                    exclude(group = "io.coil-kt", module = "coil-svg")
//                    exclude(group = "io.coil-kt", module = "coil-video")
//                    exclude(group = "io.ktor", module = "ktor-client-cio")
//                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
//                    exclude(group = "io.coil-kt.coil3", module = "coil-network-okhttp")
//                    exclude(group = "com.eygraber", module = "compose-country-code-picker")
//                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-collections-immutable-jvm")
//                }
//
//            }
//        }
    }
}

android {
    namespace = "org.nko.chat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.nko.chat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    debugImplementation(compose.uiTooling)
    //implementation(libs.desktop.jvm)
    implementation(libs.coil.compose)
    implementation(libs.ui)
    implementation(libs.foundation)
    implementation(libs.material)
    //implementation(libs.desktop)

}

compose.desktop {
    application {
        mainClass = "org.nko.chat.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.nko.chat"
            packageVersion = "1.0.0"
        }
    }
}
