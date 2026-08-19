package xyz.ksharma.krail.core.snapshot

import android.content.res.Configuration
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.preview.getAnnotation
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Base class for snapshot testing across all modules.
 *
 * To add snapshot tests to a module:
 * 1. Create a test class that extends this class
 * 2. Override [packageToScan] with your module's package name
 * 3. Annotate your @Preview functions with @ScreenshotTest
 * 4. Run ./gradlew :yourModule:recordRoborazziAndroidHostTest
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
     * Preview method names to SKIP — Robolectric hangs on composables that drive infinite
     * animations (e.g. shimmer, indeterminate loading dots) because the snapshot capture
     * never sees a stable frame. Listing them here replaces the old "documented in the
     * README" workaround, so the skip lives next to the test and won't drift.
     *
     * Match is by exact preview function name (the value the scanner reports), e.g.
     * `"PreviewLoadingDotsPill_Visible"`. Override per module:
     * ```
     * override val excludedPreviewNames = setOf(
     *     "PreviewLoadingDotsPill_Visible",
     *     "LoadingEmojiAnimPreview",
     * )
     * ```
     */
    open val excludedPreviewNames: Set<String> = emptySet()

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

        // @PreviewComponent/@PreviewScreen stack 3-4 @Preview variants ("1. Light Mode",
        // "2. Dark Mode", "3. 2x Font Scale", ...) per annotated function so the IDE's
        // preview panel shows them side by side. The scanner reports each of those
        // variants as its own entry, but capturePreviewSnapshots() below ignores every
        // variant's own uiMode/fontScale — ApplyPreviewEnvironment always drives light/
        // dark/font-scale itself — so all 3-4 variants of one preview render pixel-
        // identical input and only differ by which of the harness's own combinations
        // captures them. Without this dedupe every preview was captured 3-4x more than
        // necessary (confirmed via md5 across existing goldens: ~99% of "Light Mode" vs
        // "Dark Mode" variant pairs were byte-identical). Keep one variant per underlying
        // function — declaringClass+methodName, since private preview functions commonly
        // reuse the same name across files.
        val allPreviews = scannerWithPrivacy.getPreviews()
            .distinctBy { "${it.declaringClass}#${it.methodName}" }
        val (skipped, previews) = allPreviews.partition { it.methodName in excludedPreviewNames }

        println("Found ${allPreviews.size} previews with @ScreenshotTest in $packageToScan")
        if (skipped.isNotEmpty()) {
            // Explicit, single-line skip log per excluded preview makes drift obvious if
            // an entry in `excludedPreviewNames` no longer matches a real preview.
            println("Skipping ${skipped.size} preview(s) via excludedPreviewNames:")
            skipped.forEach { println("  - ${it.methodName}") }
        }

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
        threshold: Double,
    ) {
        val fileName = buildScreenshotFileName(preview, fontScale, isDarkMode)
        val filePath = "$screenshotsDir/$fileName.png"

        println("Capturing: $fileName")

        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)

        if (isDarkMode) {
            applyDarkMode(activityController)
        }

        activityController.setup()
        val activity = activityController.get()

        val composeView = ComposeView(activity).apply {
            setContent {
                ApplyPreviewEnvironment(fontScale, isDarkMode) {
                    preview()
                }
            }
        }

        activity.setContentView(composeView)

        settleAnimations()

        // Capture the screenshot
        composeView.captureRoboImage(
            filePath = filePath,
            roborazziOptions = SnapshotDefaults.roborazziOptions(threshold = threshold),
        )

        // Cleanup
        activityController.pause().stop().destroy()
    }

    /**
     * Runs the animation clock forward by a fixed amount before the capture, so every
     * screenshot is shot from the same point on that clock.
     *
     * `KrailTheme` drives `colors.surface` through `animateColorAsState` against a multi-stage
     * target (`taj/animations/ThemeTransitionAnimations.kt`). Capturing straight after the
     * `ComposeView` attaches raced that transition: the first capture of a preview could land
     * mid-tween and record a background one shade off, so a re-record produced a different
     * two-or-three-file set of `light_normal` drifts every time.
     *
     * Two things make the advance safe on previews whose animations never end (indeterminate
     * progress, shimmer, a spinning border) rather than only on the ones that settle:
     *
     *  - It is a **fixed duration**, not a wait for quiescence. Only the worst offenders are
     *    listed in [excludedPreviewNames]; an idle-wait would hang on every other one.
     *  - The choreographer is **paused** first. Left running, Robolectric answers
     *    `nativeScheduleVsync` immediately without moving the clock, so an endless animation
     *    re-arms a frame inside the same idle window and `ShadowLooper.idleFor` never returns
     *    (observed: one preview burning 100% CPU through 170k+ frames). Paused, a vsync is
     *    posted at the next frame boundary instead, which caps the window at
     *    [ANIMATION_SETTLE_MS] / [FRAME_DELAY_MS] frames and leaves an endless animation on a
     *    fixed, reproducible frame.
     */
    private fun settleAnimations() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_DELAY_MS))
        shadowOf(Looper.getMainLooper()).idleFor(ANIMATION_SETTLE_MS, TimeUnit.MILLISECONDS)
    }

    /**
     * Applies dark mode to the activity configuration.
     */
    private fun applyDarkMode(
        activityController: org.robolectric.android.controller.ActivityController<ComponentActivity>,
    ) {
        val config = Configuration(activityController.get().resources.configuration)
        config.uiMode = Configuration.UI_MODE_NIGHT_YES or
            (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        activityController.get().resources.updateConfiguration(
            config,
            activityController.get().resources.displayMetrics,
        )
    }

    /**
     * Provides the preview environment with proper font scale and inspection mode.
     */
    @Composable
    private fun ApplyPreviewEnvironment(
        fontScale: Float,
        isDarkMode: Boolean,
        content: @Composable () -> Unit,
    ) {
        val density = LocalDensity.current
        val customDensity = Density(
            density = density.density,
            fontScale = fontScale,
        )

        val currentConfig = LocalConfiguration.current
        val config = Configuration(currentConfig).apply {
            uiMode = if (isDarkMode) {
                Configuration.UI_MODE_NIGHT_YES or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
            } else {
                Configuration.UI_MODE_NIGHT_NO or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
            }
        }

        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalInspectionMode provides true,
            LocalConfiguration provides config,
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
        isDarkMode: Boolean,
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

        return "${baseName}_${themeText}_$scaleText"
    }

    private companion object {
        /**
         * Upper bound on the `KrailTheme` light/dark transition, rounded up.
         *
         * `ThemeTransitionTiming` stages the target colour behind `GLOW_DELAY_MS` (80) plus
         * `INTERMEDIATE_DELAY_MS` (100) before the final value is set, and the surface tween
         * that follows runs for `SURFACE_DURATION_MS` (1500): 1680 ms in total. The text
         * colours are quicker (200 ms delay plus a 350 ms tween). If those constants grow,
         * this one has to grow with them.
         */
        const val ANIMATION_SETTLE_MS = 2_000L

        /** One frame at 60 Hz, so the settle window is 125 frames of work at most. */
        const val FRAME_DELAY_MS = 16L
    }
}
