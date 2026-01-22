package xyz.ksharma.krail.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.configureDetekt() {

    // Add the formatting plugin
    dependencies.add("detektPlugins", libs.findLibrary("detekt-formatting").get())

    // Compose specific rules
    dependencies.add("detektPlugins", libs.findLibrary("detekt-compose").get())  //  }

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
            // Add other source sets when needed
        )
    }

    tasks.withType(Detekt::class.java).configureEach {
        reports.html.required.set(true)
        reports.md.required.set(true)
        jvmTarget = JvmTarget.JVM_21.target
    }
    tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
        jvmTarget = JvmTarget.JVM_21.target
    }
}
