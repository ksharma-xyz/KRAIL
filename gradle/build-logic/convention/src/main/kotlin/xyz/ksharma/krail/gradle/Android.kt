package xyz.ksharma.krail.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureAndroid(
) {
    extensions.configure<BaseExtension> {
        compileSdkVersion(AndroidVersion.COMPILE_SDK)

        defaultConfig {
            minSdk = AndroidVersion.MIN_SDK
            targetSdk = AndroidVersion.TARGET_SDK
        }

        configureJava()

        if (this is CommonExtension<*, *, *, *, *, *>) {
            buildFeatures {
                compose = true
                buildConfig = true
            }
            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

object AndroidVersion {
    const val COMPILE_SDK = 35
    const val MIN_SDK = 26 // Oreo 8.0
    const val TARGET_SDK = 35
}
