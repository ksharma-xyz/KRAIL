package xyz.ksharma.krail.core.snapshot

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.Robolectric
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.preview.getAnnotation

/**
 * Base class for snapshot testing across all modules.
 *
 * To add snapshot tests to a module:
 * 1. Create a test class that extends this class
 * 2. Override [packageToScan] with your module's package name
 * 3. Annotate your @Preview functions with @ScreenshotTest
 * 4. Run ./gradlew :yourModule:recordRoborazziDebug
 *
 * Example:
 * ```
 * @RunWith(RobolectricTestRunner::class)
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6)
 * class MyModuleSnapshotTest : BaseSnapshotTest() {
 *     override val packageToScan = "com.example.mymodule"
 * }
 * ```
 */
abstract class BaseSnapshotTest {

    /**
     * The package name to scan for @Preview + @ScreenshotTest annotations.
     * Override this in your test class.
     *
     * Example: "xyz.ksharma.krail.taj"
     */
    abstract val packageToScan: String

    /**
     * Screenshot directory relative to module root.
     * Default: "screenshots"
     */
    open val screenshotsDir: String = "screenshots"

    /**
     * Whether to include private preview functions.
     * Default: true
     */
    open val includePrivatePreviews: Boolean = true

    /**
     * Custom font scales for light mode.
     * Default: Uses [SnapshotDefaults.lightModeFontScales]
     */
    open val lightModeFontScales: List<Float>
        get() = SnapshotDefaults.lightModeFontScales

    /**
     * Custom font scales for dark mode.
     * Default: Uses [SnapshotDefaults.darkModeFontScales]
     */
    open val darkModeFontScales: List<Float>
        get() = SnapshotDefaults.darkModeFontScales

    /**
     * Whether to test dark mode.
     * Default: true
     */
    open val testDarkMode: Boolean
        get() = SnapshotDefaults.testDarkMode

    /**
     * Main test method that scans and generates all snapshots.
     * Call this from your @Test method.
     */
    protected fun generateSnapshots() {
        val scanner = AndroidComposablePreviewScanner()
            .scanPackageTrees(packageToScan)
            .includeAnnotationInfoForAllOf(ScreenshotTest::class.java)
            .includeIfAnnotatedWithAnyOf(ScreenshotTest::class.java)

        val scannerWithPrivacy = if (includePrivatePreviews) {
            scanner.includePrivatePreviews()
        } else {
            scanner
        }

        val previews = scannerWithPrivacy.getPreviews()

        println("✅ Found ${previews.size} previews with @ScreenshotTest in $packageToScan")

        previews.forEach { preview ->
            capturePreviewSnapshots(preview)
        }
    }

    /**
     * Captures all configured snapshot variations for a preview.
     */
    private fun capturePreviewSnapshots(preview: ComposablePreview<AndroidPreviewInfo>) {
        val screenshotConfig = preview.getAnnotation<ScreenshotTest>()
        val threshold = screenshotConfig?.threshold ?: SnapshotDefaults.defaultThreshold

        val modes = buildList {
            add(false to lightModeFontScales)
            if (testDarkMode) {
                add(true to darkModeFontScales)
            }
        }

        modes.forEach { (isDarkMode, fontScales) ->
            fontScales.forEach { fontScale ->
                captureScreenshot(preview, fontScale, isDarkMode, threshold)
            }
        }
    }

    /**
     * Captures a single screenshot with the specified configuration.
     */
    private fun captureScreenshot(
        preview: ComposablePreview<AndroidPreviewInfo>,
        fontScale: Float,
        isDarkMode: Boolean,
        threshold: Double
    ) {
        val fileName = buildScreenshotFileName(preview, fontScale, isDarkMode)
        val filePath = "$screenshotsDir/$fileName.png"

        println("📷 Capturing: $fileName")

        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)

        if (isDarkMode) {
            applyDarkMode(activityController)
        }

        activityController.setup()
        val activity = activityController.get()

        val composeView = ComposeView(activity).apply {
            setContent {
                ApplyPreviewEnvironment(fontScale) {
                    preview()
                }
            }
        }

        activity.setContentView(composeView)

        composeView.captureRoboImage(
            filePath = filePath,
            roborazziOptions = SnapshotDefaults.roborazziOptions(threshold = threshold)
        )

        activityController.pause().stop().destroy()
    }

    /**
     * Applies dark mode to the activity configuration.
     */
    private fun applyDarkMode(activityController: org.robolectric.android.controller.ActivityController<ComponentActivity>) {
        val config = Configuration(activityController.get().resources.configuration)
        config.uiMode = Configuration.UI_MODE_NIGHT_YES or
                (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        activityController.get().resources.updateConfiguration(
            config,
            activityController.get().resources.displayMetrics
        )
    }

    /**
     * Provides the preview environment with proper font scale and inspection mode.
     */
    @Composable
    private fun ApplyPreviewEnvironment(
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        val density = LocalDensity.current
        val customDensity = Density(
            density = density.density,
            fontScale = fontScale
        )

        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalInspectionMode provides true
        ) {
            content()
        }
    }

    /**
     * Builds a screenshot file name from preview metadata.
     * Format: {PreviewName}_{theme}_{fontScale}
     */
    private fun buildScreenshotFileName(
        preview: ComposablePreview<AndroidPreviewInfo>,
        fontScale: Float,
        isDarkMode: Boolean
    ): String {
        val baseName = AndroidPreviewScreenshotIdBuilder(preview)
            .ignoreClassName()
            .build()

        val themeText = if (isDarkMode) "dark" else "light"
        val scaleText = when (fontScale) {
            1.0f -> "normal"
            1.5f -> "large"
            2.0f -> "xlarge"
            else -> "scale_${fontScale}x"
        }

        return "${baseName}_${themeText}_${scaleText}"
    }
}

