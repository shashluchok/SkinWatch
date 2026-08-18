import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

compose.resources {
    packageOfResClass = "com.shashluchok.skinwatch.resources"
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.shashluchok.skinwatch.shared"
        // Plain `compileSdk = 37` resolves to the SDK Manager target "android-37", but the
        // installed platform for API 37 is "android-37.0" (Android's new major.minor SDK
        // release scheme -- see https://developer.android.com/build/releases/agp-9-0-0-release-notes).
        compileSdk {
            version =
                release(
                    libs.versions.android.compileSdk
                        .get()
                        .toInt(),
                ) {
                    minorApiLevel =
                        libs.versions.android.compileSdkMinor
                            .get()
                            .toInt()
                }
        }
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.androidx.work.runtimeKtx)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.compose.animation)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation3.ui)
            implementation(libs.compottie)
            implementation(libs.coil.compose)
            implementation(libs.coil.networkKtor3)
            implementation(libs.vico.composeM3)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.androidx.room3.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.ktor.client.mock)
        }
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspJs", libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}

detekt {
    // The detekt Gradle plugin only auto-detects JVM/Android source sets;
    // Kotlin Multiplatform source sets (commonMain, iosMain, ...) need to be wired explicitly,
    // otherwise the `detekt` task reports NO-SOURCE. Deferred via a provider so
    // hierarchy-template source sets (e.g. webMain) are resolved before this reads them.
    // Generated Compose resource accessors are excluded from the input set itself (rather
    // than just via task-level `exclude`) so Gradle doesn't demand an explicit task
    // dependency on the generator tasks that produce them.
    source.setFrom(
        provider {
            kotlin.sourceSets
                .flatMap { it.kotlin.srcDirs }
                .filterNot { it.path.contains("${File.separator}generated${File.separator}") }
        },
    )
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
