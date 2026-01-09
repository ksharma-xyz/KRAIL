package xyz.ksharma.krail.gradle

import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class MapLibreConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalSpmForKmpFeature::class)
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.frankois944.spmForKmp")

        val maplibreNativeDistributionSpmVersion =
            libs.findVersion("maplibreNativeDistributionSpm").get().requiredVersion

        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinNativeTarget>().configureEach {
                // Configure the package root for the cinterop named "spmMaplibre"
                swiftPackageConfig("spmMaplibre") {
                    dependency {
                        remotePackageVersion(
                            url = uri("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                            products = { add("MapLibre") },
                            version = maplibreNativeDistributionSpmVersion
                        )
                    }
                }
            }
        }
    }
}
