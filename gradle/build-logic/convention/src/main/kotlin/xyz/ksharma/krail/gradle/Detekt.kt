package xyz.ksharma.krail.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.configureDetekt() {

    // Add the formatting plugin
    dependencies.add("detektPlugins", libs.findLibrary("detekt-formatting").get())

    // Compose specific rules
    dependencies.add("detektPlugins", libs.findLibrary("detekt-compose").get())  //  }

    // KRAIL's own custom rules (see gradle/build-logic/detekt-rules), e.g. PublicImplementationClass.
    // A GAV string, not project(":detekt-rules") — that project lives in the separate
    // includeBuild("gradle/build-logic") composite build, not in this (the root) build's project
    // graph, so it has to be requested by coordinate and resolved via composite substitution.
    dependencies.add("detektPlugins", "xyz.ksharma.krail.gradle:detekt-rules:unspecified")

    extensions.configure<DetektExtension>("detekt") {
        autoCorrect = true
        buildUponDefaultConfig = true
        allRules = false
        // Use rootProject.projectDir for the shared config
        config.setFrom("${rootProject.projectDir}/config/detekt.yml")
        baseline = file("$projectDir/baseline.xml")
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
            // Test code is analysed too. A test is code a person has to read and change, and
            // the rules that keep production readable keep tests readable. The rules that only
            // make sense for production (magic numbers, line length, class and function counts,
            // duplicate string literals) are exempted for test directories in config/detekt.yml
            // rather than by leaving the sources unscanned. Directory names are the KMP source
            // set names, not AGP's: `androidHostTest` is what `withHostTest {}` creates.
            "src/commonTest/kotlin",
            "src/androidHostTest/kotlin",
            "src/iosTest/kotlin",
            "src/androidInstrumentedTest/kotlin",
        )
    }

    // The custom krail rules read shrinking ratchet baselines (config/*-baseline.txt) directly
    // from disk, because a detekt rule is given no project metadata to find them through. They
    // are not part of detekt's own config, so Gradle has to be told they are task inputs —
    // otherwise deleting an entry leaves every detekt task UP-TO-DATE and the violation that
    // just stopped being grandfathered goes unreported until something else edits that module.
    val ratchetBaselines = rootProject.fileTree(
        mapOf("dir" to "config", "include" to "*-baseline.txt"),
    )

    tasks.withType(Detekt::class.java).configureEach {
        reports.html.required.set(true)
        reports.md.required.set(true)
        jvmTarget = JvmTarget.JVM_21.target
        inputs.files(ratchetBaselines)
            .withPropertyName("krailRatchetBaselines")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
    tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
        jvmTarget = JvmTarget.JVM_21.target
    }
}
