
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
    // Applied (not `apply false`) so the root project owns the merged coverage report. Both the
    // per-module wiring and this project's filters and floor are configured from Coverage.kt, so
    // a module report and the merged report exclude exactly the same things. Do not add a
    // `kover { }` block here — it would apply to the merged report only, which is the split that
    // let the two drift apart in the first place.
    alias(libs.plugins.kover)
}
