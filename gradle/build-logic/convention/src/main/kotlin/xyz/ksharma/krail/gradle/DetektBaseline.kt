package xyz.ksharma.krail.gradle

import org.gradle.api.Project

fun Project.configureDetektBaseline() {
    // Only apply this to the root project
    require(this == rootProject) {
        "generateAllDetektBaselines should only be applied to the root project"
    }

    tasks.register("generateAllDetektBaselines") {
        group = "verification"
        description = "Generates Detekt baseline files for all subprojects"

        // This will find all detektBaseline tasks in all subprojects
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("detektBaseline") })
    }
}