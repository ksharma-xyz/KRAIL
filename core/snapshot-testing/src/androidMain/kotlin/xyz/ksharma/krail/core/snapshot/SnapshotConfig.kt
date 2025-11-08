package xyz.ksharma.krail.core.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Default configuration for all snapshot tests in KRAIL.
 * These settings apply to all screenshots unless overridden per-preview.
 *
 * Default behavior generates 3 screenshots per preview:
 * 1. Light mode - 1.0f font scale
 * 2. Light mode - 2.0f font scale
 * 3. Dark mode - 1.0f font scale
 */
object SnapshotDefaults {

    /**
     * Default font scales to test in light mode.
     * Default: 1.0f and 2.0f
     */
    val lightModeFontScales = listOf(1.0f, 2.0f)

    /**
     * Default font scales to test in dark mode.
     * Default: 1.0f only
     */
    val darkModeFontScales = listOf(1.0f)

    /**
     * Default device configuration (portrait phone by default)
     */
    const val defaultDevice = RobolectricDeviceQualifiers.Pixel6

    /**
     * Whether to test dark mode by default
     */
    const val testDarkMode = true

    /**
     * Default comparison threshold (0.0 = exact match)
     */
    const val defaultThreshold = 0.0

    /**
     * Default SDK version for tests
     */
    const val defaultSdk = 34

    /**
     * Device configurations for different form factors
     */
    object Devices {
        const val PHONE_PORTRAIT = RobolectricDeviceQualifiers.Pixel6
        const val PHONE_LANDSCAPE = "$PHONE_PORTRAIT-land"
        const val TABLET_PORTRAIT = RobolectricDeviceQualifiers.MediumTablet
        const val TABLET_LANDSCAPE = "$TABLET_PORTRAIT-land"
    }

    /**
     * Roborazzi default options
     *
     * @param threshold Comparison threshold (0.0 = exact match, 0.01 = 1% difference allowed)
     * @param resizeScale Scale factor for screenshot size (1.0 = full size, 0.5 = half size)
     */
    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    fun roborazziOptions(
        threshold: Double = defaultThreshold,
        resizeScale: Double = 1.0
    ) = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            changeThreshold = threshold.toFloat()
        ),
        recordOptions = RoborazziOptions.RecordOptions(
            resizeScale = resizeScale
        )
    )
}

/**
 * Configuration for specific screenshot test scenarios
 */
data class SnapshotTestConfig(
    val lightModeFontScales: List<Float> = SnapshotDefaults.lightModeFontScales,
    val darkModeFontScales: List<Float> = SnapshotDefaults.darkModeFontScales,
    val devices: List<String> = listOf(SnapshotDefaults.defaultDevice),
    val testDarkMode: Boolean = SnapshotDefaults.testDarkMode,
    val threshold: Double = SnapshotDefaults.defaultThreshold,
    val animationTimestamps: List<Long> = emptyList() // For animation testing
)

