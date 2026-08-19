
// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.firebase.crashlyticsPlugin) apply false
    alias(libs.plugins.firebase.performancePlugin) apply false
    alias(libs.plugins.wire) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false
    // Applied (not `apply false`) so the root project owns the merged coverage report. The
    // per-module side is wired from KotlinMultiplatformConventionPlugin — see Coverage.kt.
    alias(libs.plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                // Compose compiler emits one of these per file holding `@Composable` lambdas.
                // Never written by hand, never meaningfully "covered".
                classes("*.ComposableSingletons*")
                // SQLDelight codegen. The hand-written repositories above it are measured.
                packages("xyz.ksharma.krail.sandook.sandook")
                // BuildKonfig codegen.
                classes("*.BuildKonfig")
                // Compose Resources accessors (`Res.drawable.*`), also codegen.
                packages("*.generated.resources")
            }
        }
    }
}
