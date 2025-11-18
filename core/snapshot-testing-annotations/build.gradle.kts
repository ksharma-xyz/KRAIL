plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
}

kotlin {
    jvm()  // For Android compatibility via JVM target

    iosArm64()
    iosSimulatorArm64()


    sourceSets {
        commonMain {
            dependencies {
                // No dependencies! Just a simple annotation
            }
        }
    }
}

