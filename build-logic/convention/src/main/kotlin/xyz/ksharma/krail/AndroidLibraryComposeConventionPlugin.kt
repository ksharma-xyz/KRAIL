import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

@Suppress("UNUSED")
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val javaVersion = libs.findVersion("java").get().toString().toInt()
            val minSdkVersion = libs.findVersion("minSdk").get().toString().toInt()
            val compileSdkVersion = libs.findVersion("compileSdk").get().toString().toInt()

            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = compileSdkVersion

                defaultConfig {
                    minSdk = minSdkVersion
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.values()[javaVersion - 1]
                    targetCompatibility = JavaVersion.values()[javaVersion - 1]
                }

                (this as ExtensionAware).configure<KotlinJvmOptions> {
                    jvmTarget = "$javaVersion"
                    freeCompilerArgs = freeCompilerArgs + listOf(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlinx.coroutines.FlowPreview",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
                    )
                }

                buildFeatures {
                    compose = true
                    buildConfig = true
                }
            }
        }
    }
}

Kotlin Gradle Plugin: The KotlinJvmOptions class has been replaced by the kotlinOptions property in the KotlinJvmCompilerOptions class. You can access and configure JVM options using this property.
Kotlin Compiler: The KotlinJvmOptions class has been replaced by the jvmTarget property in the KotlinCompilerOptions class. You can set the JVM target version using this property.
