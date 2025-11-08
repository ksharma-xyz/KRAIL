package xyz.ksharma.krail.core.snapshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * Base helper for snapshot testing.
 * Provides utilities for capturing screenshots with consistent configuration.
 */
object SnapshotTestHelper {

    /**
     * Builds a screenshot file name from preview info and configuration.
     *
     * Format: {ClassName}_{PreviewName}_{theme}_{fontScale}.png
     * Example: TextField_Preview_light_normal.png
     */
    fun buildScreenshotFileName(
        preview: ComposablePreview<AndroidPreviewInfo>,
        fontScale: Float = 1.0f,
        isDarkMode: Boolean = false,
        variant: String = ""
    ): String {
        val className = preview.declaringClass.substringAfterLast(".")
        val methodName = preview.methodName
        val previewName = preview.previewInfo.name.takeIf { it.isNotBlank() } ?: methodName

        val themeText = if (isDarkMode) "dark" else "light"

        val scaleText = when (fontScale) {
            1.0f -> "normal"
            1.5f -> "large"
            2.0f -> "xlarge"
            else -> "scale_${fontScale}x"
        }

        val variantSuffix = if (variant.isNotBlank()) "_$variant" else ""

        return "${className}_${previewName}_${themeText}_${scaleText}${variantSuffix}"
            .replace(" ", "_")
            .replace("[^a-zA-Z0-9_]".toRegex(), "")
    }

    /**
     * Provides preview environment with inspection mode and custom font scale.
     */
    @Composable
    fun ProvidePreviewEnvironment(
        fontScale: Float = 1.0f,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalInspectionMode provides true,
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = fontScale
            )
        ) {
            content()
        }
    }

    /**
     * Captures a screenshot with the given configuration.
     *
     * @param composeTestRule The compose test rule
     * @param preview The composable preview to capture
     * @param fontScale Font scale to use
     * @param isDarkMode Whether to use dark mode
     * @param screenshotsDir Base directory for screenshots (relative to module root)
     * @param roborazziOptions Roborazzi options for comparison
     */
    fun captureScreenshot(
        composeTestRule: ComposeContentTestRule,
        preview: ComposablePreview<AndroidPreviewInfo>,
        fontScale: Float = 1.0f,
        isDarkMode: Boolean = false,
        screenshotsDir: String = "screenshots",
        roborazziOptions: com.github.takahirom.roborazzi.RoborazziOptions = SnapshotDefaults.roborazziOptions()
    ) {
        val fileName = buildScreenshotFileName(preview, fontScale, isDarkMode)
        val filePath = "$screenshotsDir/$fileName.png"

        composeTestRule.setContent {
            ProvidePreviewEnvironment(fontScale = fontScale) {
                preview()
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = filePath,
            roborazziOptions = roborazziOptions
        )
    }

    /**
     * Captures screenshots with default configuration:
     * - Light mode: 1.0f and 2.0f font scales
     * - Dark mode: 1.0f font scale
     * Total: 3 screenshots per preview
     */
    fun captureWithDefaults(
        composeTestRule: ComposeContentTestRule,
        preview: ComposablePreview<AndroidPreviewInfo>,
        screenshotsDir: String = "screenshots",
        threshold: Double = SnapshotDefaults.defaultThreshold,
        lightModeFontScales: List<Float> = SnapshotDefaults.lightModeFontScales,
        darkModeFontScales: List<Float> = SnapshotDefaults.darkModeFontScales,
        testDarkMode: Boolean = SnapshotDefaults.testDarkMode
    ) {
        val roborazziOptions = SnapshotDefaults.roborazziOptions(threshold = threshold)

        // Capture light mode screenshots
        lightModeFontScales.forEach { fontScale ->
            captureScreenshot(
                composeTestRule = composeTestRule,
                preview = preview,
                fontScale = fontScale,
                isDarkMode = false,
                screenshotsDir = screenshotsDir,
                roborazziOptions = roborazziOptions
            )
        }

        // Capture dark mode screenshots
        if (testDarkMode) {
            darkModeFontScales.forEach { fontScale ->
                captureScreenshot(
                    composeTestRule = composeTestRule,
                    preview = preview,
                    fontScale = fontScale,
                    isDarkMode = true,
                    screenshotsDir = screenshotsDir,
                    roborazziOptions = roborazziOptions
                )
            }
        }
    }
}

