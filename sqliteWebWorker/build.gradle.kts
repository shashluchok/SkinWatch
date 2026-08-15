@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    js {
        browser()
        useEsModules()
    }

    wasmJs {
        browser()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation` -- `shared`'s webMain consumes the `WebWorkerSQLiteDriver`
            // type this module's `createSqliteWebWorkerDriver()` returns, so it must be re-exported.
            api(libs.androidx.sqlite.web)
            // Local npm package (not a registry dependency) -- points at ./worker, which contains
            // worker.js + package.json. `useEsModules()` above is required for this worker's
            // `import.meta.url`-based script resolution to work.
            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
        }
        wasmJsMain.dependencies {
            // JS gets `org.w3c.dom.Worker` from the Kotlin/JS stdlib directly; WasmJs needs this
            // multiplatform-safe DOM interop library for the same type.
            implementation(libs.kotlinx.browser)
        }
    }
}

detekt {
    // See shared/build.gradle.kts for why this is needed in Kotlin Multiplatform modules.
    source.setFrom(
        provider {
            kotlin.sourceSets
                .flatMap { it.kotlin.srcDirs }
                .filterNot { it.path.contains("${File.separator}generated${File.separator}") }
        },
    )
}
