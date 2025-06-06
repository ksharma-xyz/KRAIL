package xyz.ksharma.krail.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.configureDetekt() {
    extensions.configure<DetektExtension>("detekt") {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$projectDir/config/detekt.yml")
        baseline = file("$projectDir/config/baseline.xml")
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
            // Add other source sets when needed
        )
    }

    tasks.withType(Detekt::class.java).configureEach {
        reports.html.required.set(true)
        reports.md.required.set(true)
        jvmTarget = JvmTarget.JVM_17.target
    }
    tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
        jvmTarget = JvmTarget.JVM_17.target
    }
}
