# Snapshot Testing Approaches for KRAIL - Complete Comparison

## Your Requirements ✅
1. ✅ Add annotation to specific previews only (selective screenshot generation)
2. ✅ Previews in `commonMain` should generate screenshots
3. ✅ No need to write previews elsewhere
4. ✅ Work with existing `@Preview` annotations

---

## Approach Comparison

| Feature | **Roborazzi + ComposablePreviewScanner** | **Google Compose Screenshot Testing** |
|---------|------------------------------------------|----------------------------------------|
| **Maturity** | ⭐⭐⭐⭐⭐ Production-ready | ⭐⭐ Experimental (Alpha) |
| **Maintenance** | Community-driven, actively maintained | Google official, uncertain future |
| **Setup Complexity** | Medium (more boilerplate) | Low (minimal setup) |
| **Compose Multiplatform** | ✅ Full support | ⚠️ Limited support |
| **Selective Screenshots** | ✅ YES - Custom annotations | ❌ NO - All previews or none |
| **Preview Location** | ✅ commonMain (via scanning) | ❌ Separate screenshotTest source |
| **Configuration** | ✅ Per-preview config via annotations | ⚠️ Limited config |
| **CI/CD Integration** | ✅ Gradle tasks + flexible | ✅ Gradle tasks |
| **File Size** | PNG images | PNG images |
| **Comparison Options** | ✅ Threshold, regions, custom | ⚠️ Limited |
| **Monthly Downloads** | 150k+ | Unknown (new) |
| **iOS Support** | ❌ JVM only | ❌ Android only |

---

## ⭐ RECOMMENDED: Roborazzi + ComposablePreviewScanner

### Why This Approach?

✅ **Meets ALL your requirements:**
- ✅ Custom annotation for selective screenshots
- ✅ Scans `commonMain` previews directly
- ✅ No duplicate preview code
- ✅ Production-ready and stable

✅ **Additional Benefits:**
- Flexible configuration per preview
- Active community support
- Used by major projects
- Better error messages
- More control over comparison logic

---

## Setup: Roborazzi + ComposablePreviewScanner

### 1. Add Dependencies to `libs.versions.toml`

```toml
[versions]
roborazzi = "1.31.0"
composablePreviewScanner = "0.7.1"
robolectric = "4.14.1"

[libraries]
# Screenshot Testing
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }
preview-scanner-android = { module = "io.github.sergio-sastre.ComposablePreviewScanner:android", version.ref = "composablePreviewScanner" }
test-robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }

[plugins]
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

### 2. Add Plugin to Root `build.gradle.kts`

```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.roborazzi) apply false
}
```

### 3. Configure `composeApp/build.gradle.kts`

```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.roborazzi)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        
        // Required for screenshot tests
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    
    sourceSets {
        // ...existing sourceSets...
        
        androidMain.dependencies {
            // ...existing dependencies...
        }
        
        commonMain.dependencies {
            // ...existing dependencies...
        }
        
        // Add test dependencies
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
                implementation(libs.roborazzi.junit)
                implementation(libs.preview.scanner.android)
                implementation(libs.test.robolectric)
                implementation(libs.test.junit)
                implementation(libs.test.kotlin)
            }
        }
    }
}
```

### 4. Create Custom Annotation (Your Selective Marker!)

Create: `taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/screenshot/ScreenshotTest.kt`

```kotlin
package xyz.ksharma.krail.taj.screenshot

/**
 * Annotation to mark @Preview composables for screenshot testing.
 * Only previews with this annotation will generate screenshots.
 * 
 * Usage:
 * ```
 * @ScreenshotTest
 * @Preview
 * @Composable
 * fun MyComponentPreview() { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenshotTest(
    /**
     * Optional: Comparison threshold (0.0 to 1.0)
     * Default is 0.0 (exact match)
     */
    val threshold: Double = 0.0,
    
    /**
     * Optional: Description for documentation
     */
    val description: String = ""
)
```

### 5. Annotate Your Previews (Only What You Want!)

```kotlin
// In your existing commonMain files
import xyz.ksharma.krail.taj.screenshot.ScreenshotTest

// ✅ This will generate a screenshot
@ScreenshotTest
@Preview
@Composable
private fun SavedTripCardPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            trip = Trip(
                fromStopId = "1",
                fromStopName = "Edmondson Park Station",
                toStopId = "2",
                toStopName = "Harris Park Station",
            ),
            primaryTransportMode = TransportMode.Train(),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

// ❌ This will NOT generate a screenshot (no @ScreenshotTest)
@Preview
@Composable
private fun SomeOtherPreview() {
    // ...
}

// ✅ With custom threshold for slight variations
@ScreenshotTest(threshold = 0.01, description = "Allows 1% difference")
@Preview
@Composable
private fun DynamicContentPreview() {
    // ...
}
```

### 6. Create Screenshot Test File

Create: `composeApp/src/androidUnitTest/kotlin/xyz/ksharma/krail/screenshot/PreviewScreenshotTest.kt`

```kotlin
package xyz.ksharma.krail.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.RoborazziOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import xyz.ksharma.krail.taj.screenshot.ScreenshotTest
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Screenshot tests for all @Preview composables annotated with @ScreenshotTest
 * 
 * Run:
 * - Record: ./gradlew composeApp:recordRoborazziDebug
 * - Verify: ./gradlew composeApp:verifyRoborazziDebug
 * - Compare: ./gradlew composeApp:compareRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6
)
class PreviewScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        // Scan for all previews in commonMain that have @ScreenshotTest annotation
        private val screenshotPreviews: List<ComposablePreview<AndroidPreviewInfo>> by lazy {
            AndroidComposablePreviewScanner()
                .scanPackageTrees(
                    "xyz.ksharma.krail"  // Scan your entire app package
                )
                .includeIfAnnotatedWithAnyOf(
                    ScreenshotTest::class.java  // Only previews with this annotation
                )
                .includeAnnotationInfoForAllOf(
                    ScreenshotTest::class.java  // Include annotation config
                )
                .getPreviews()
        }
    }

    @Test
    fun generateAllScreenshots() {
        screenshotPreviews.forEach { preview ->
            val screenshotConfig = preview.getAnnotation<ScreenshotTest>()
            val fileName = buildScreenshotFileName(preview)
            
            composeTestRule.setContent {
                ProvidePreviewEnvironment {
                    preview.invoke()
                }
            }

            composeTestRule.onRoot().captureRoboImage(
                filePath = "screenshots/$fileName.png",
                roborazziOptions = RoborazziOptions(
                    compareOptions = RoborazziOptions.CompareOptions(
                        resultValidator = when {
                            screenshotConfig?.threshold != null && screenshotConfig.threshold > 0 -> {
                                RoborazziOptions.CompareOptions.ThresholdValidator(screenshotConfig.threshold)
                            }
                            else -> RoborazziOptions.CompareOptions.ThresholdValidator(0.0)
                        }
                    )
                )
            )
        }
    }

    /**
     * Provides preview environment with inspection mode
     */
    @Composable
    private fun ProvidePreviewEnvironment(content: @Composable () -> Unit) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalInspectionMode provides true
        ) {
            KrailTheme {
                content()
            }
        }
    }

    /**
     * Build screenshot file name from preview info
     */
    private fun buildScreenshotFileName(preview: ComposablePreview<AndroidPreviewInfo>): String {
        val className = preview.declaringClass.substringAfterLast(".")
        val methodName = preview.methodName
        val previewName = preview.previewInfo.name.takeIf { it.isNotBlank() } ?: methodName
        return "${className}_${previewName}"
            .replace(" ", "_")
            .replace("[^a-zA-Z0-9_]".toRegex(), "")
    }
}
```

---

## Usage: Your Daily Workflow

### 1. Mark Previews for Screenshot (One-Time)

```kotlin
// In ANY commonMain file
@ScreenshotTest  // ← Add this!
@Preview
@Composable
fun MyComponentPreview() {
    PreviewTheme {
        MyComponent()
    }
}
```

### 2. Record Screenshots (When UI Changes)

```bash
# Generate screenshots for ALL @ScreenshotTest previews
./gradlew composeApp:recordRoborazziDebug
```

**Output:** Screenshots saved to `composeApp/build/outputs/roborazzi/`

### 3. Verify Screenshots (Before Commit)

```bash
# Check if current UI matches recorded screenshots
./gradlew composeApp:verifyRoborazziDebug
```

**Result:**
- ✅ Pass: UI matches screenshots
- ❌ Fail: Shows diff images

### 4. Compare/Review Differences

```bash
# Generate comparison report
./gradlew composeApp:compareRoborazziDebug
```

**Output:** HTML report in `composeApp/build/reports/roborazzi/`

### 5. Commit Screenshots to Git

```bash
git add composeApp/build/outputs/roborazzi/
git commit -m "feat: update transport mode icons"
```

---

## Real Examples from Your Codebase

### Example 1: Transport Mode Icons

```kotlin
// feature/trip-planner/ui/src/commonMain/kotlin/.../TransportModeIcon.kt

@ScreenshotTest(description = "Train icon default size")
@Preview(group = "Transport Mode Icons")
@Composable
private fun TrainPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Train(),
            displayBorder = false,
        )
    }
}

@ScreenshotTest(description = "Train icon large font")
@Preview(group = "Transport Mode Icons")
@Composable
private fun TrainPreviewLarge() {
    PreviewTheme(fontScale = 2.0f) {
        TransportModeIcon(
            transportMode = TransportMode.Train(),
            displayBorder = false,
        )
    }
}

// Don't want screenshot for this? Just don't add @ScreenshotTest!
@Preview(group = "Transport Mode Icons")
@Composable
private fun BusPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Bus(),
            displayBorder = false,
        )
    }
}
```

### Example 2: Saved Trip Cards

```kotlin
// feature/trip-planner/ui/src/commonMain/kotlin/.../SavedTripCard.kt

@ScreenshotTest
@Preview
@Composable
private fun SavedTripCardPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            trip = Trip(
                fromStopId = "1",
                fromStopName = "Edmondson Park Station",
                toStopId = "2",
                toStopName = "Harris Park Station",
            ),
            primaryTransportMode = TransportMode.Train(),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@ScreenshotTest(threshold = 0.01)  // Allow 1% difference
@Preview
@Composable
private fun SavedTripCardListPreview() {
    PreviewTheme {
        Column {
            SavedTripCard(/* ... */)
            SavedTripCard(/* ... */)
        }
    }
}
```

---

## Advanced Configuration

### Default Configuration (Top-Level)

Create a configuration object for default settings that apply to all screenshots unless overridden:

Create: `composeApp/src/androidUnitTest/kotlin/xyz/ksharma/krail/screenshot/ScreenshotConfig.kt`

```kotlin
package xyz.ksharma.krail.screenshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Default configuration for all screenshot tests.
 * These settings apply to all screenshots unless overridden per-preview.
 */
object ScreenshotDefaults {
    
    /**
     * Default font scales to test for accessibility.
     * Tests will be generated for each font scale automatically.
     */
    val fontScales = listOf(1.0f, 1.5f, 2.0f)
    
    /**
     * Default device configurations (portrait phone by default)
     */
    val defaultDevice = RobolectricDeviceQualifiers.Pixel6
    
    /**
     * Whether to test dark mode by default
     */
    val testDarkMode = false
    
    /**
     * Default comparison threshold (0.0 = exact match)
     */
    val defaultThreshold = 0.0
    
    /**
     * Default SDK version for tests
     */
    val defaultSdk = 34
    
    /**
     * Whether to test landscape orientation
     */
    val testLandscape = false
    
    /**
     * Whether to test tablet sizes
     */
    val testTablet = false
    
    /**
     * Device configurations for different form factors
     */
    object Devices {
        const val PHONE_PORTRAIT = RobolectricDeviceQualifiers.Pixel6
        const val PHONE_LANDSCAPE = RobolectricDeviceQualifiers.Pixel6 + "-land"
        const val TABLET_PORTRAIT = RobolectricDeviceQualifiers.NexusTablet
        const val TABLET_LANDSCAPE = RobolectricDeviceQualifiers.NexusTablet + "-land"
    }
    
    /**
     * Roborazzi default options
     */
    fun defaultRoborazziOptions(threshold: Double = defaultThreshold) = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            resultValidator = RoborazziOptions.CompareOptions.ThresholdValidator(threshold)
        ),
        recordOptions = RoborazziOptions.RecordOptions(
            resizeScale = 1.0  // 1.0 = actual size, 0.5 = half size for smaller files
        )
    )
}

/**
 * Configuration for specific screenshot test scenarios
 */
data class ScreenshotTestConfig(
    val fontScales: List<Float> = ScreenshotDefaults.fontScales,
    val devices: List<String> = listOf(ScreenshotDefaults.defaultDevice),
    val testDarkMode: Boolean = ScreenshotDefaults.testDarkMode,
    val threshold: Double = ScreenshotDefaults.defaultThreshold,
    val animationTimestamps: List<Long> = emptyList() // For animation testing
)
```

### Per-Preview Configuration with Overrides

```kotlin
// ✅ Default behavior: Tests at 1.0f, 1.5f, 2.0f font scales
@ScreenshotTest
@Preview
@Composable
fun DefaultScalesPreview() {
    PreviewTheme {
        MyComponent()
    }
}

// ✅ Override: Only test at 1.0f and 2.0f
@ScreenshotTest(description = "Only normal and 2x font")
@Preview
@Composable
fun CustomScalesPreview() {
    PreviewTheme(fontScale = 1.0f) {  // Specify in preview
        MyComponent()
    }
}

// ✅ Exact match (strict comparison)
@ScreenshotTest
@Preview
@Composable
fun StrictPreview() {
    PreviewTheme {
        StaticComponent()
    }
}

// ✅ Allow 1% difference (for dynamic content, slight variations)
@ScreenshotTest(threshold = 0.01)
@Preview
@Composable
fun FlexiblePreview() {
    PreviewTheme {
        ComponentWithAnimation()
    }
}

// ✅ With documentation
@ScreenshotTest(
    threshold = 0.005,
    description = "Search results with loading state - allow 0.5% diff"
)
@Preview
@Composable
fun DocumentedPreview() {
    PreviewTheme {
        SearchResults()
    }
}
```

### Device & Orientation Configuration

```kotlin
// ✅ Portrait phone (default)
@ScreenshotTest
@Preview
@Composable
fun PhonePortraitPreview() {
    PreviewTheme {
        MyScreen()
    }
}

// ✅ Landscape phone
@ScreenshotTest(description = "Landscape mode")
@Preview(device = "spec:width=891dp,height=411dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun PhoneLandscapePreview() {
    PreviewTheme {
        MyScreen()
    }
}

// ✅ Tablet portrait
@ScreenshotTest(description = "Tablet 10-inch")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun TabletPreview() {
    PreviewTheme {
        MyScreen()
    }
}

// ✅ Tablet landscape
@ScreenshotTest(description = "Tablet landscape")
@Preview(device = "spec:width=1920dp,height=1200dp,dpi=240,orientation=landscape")
@Composable
fun TabletLandscapePreview() {
    PreviewTheme {
        MyScreen()
    }
}
```

### Dark Mode Configuration

```kotlin
// ✅ Light mode (default)
@ScreenshotTest
@Preview
@Composable
fun LightModePreview() {
    PreviewTheme(darkTheme = false) {
        MyComponent()
    }
}

// ✅ Dark mode
@ScreenshotTest(description = "Dark mode")
@Preview
@Composable
fun DarkModePreview() {
    PreviewTheme(darkTheme = true) {
        MyComponent()
    }
}

// ✅ Test both light and dark with multi-preview
@Preview(name = "Light Mode")
@Preview(name = "Dark Mode")
@Composable
fun BothModesPreview() {
    // This will generate 2 screenshots automatically
    PreviewTheme {
        MyComponent()
    }
}
```

### Animation Testing (Frame-by-Frame)

Roborazzi supports capturing animations at different timestamps. Great for testing loading states, transitions, etc.

```kotlin
// Create enhanced test file for animation support
// composeApp/src/androidUnitTest/kotlin/xyz/ksharma/krail/screenshot/AnimationScreenshotTest.kt

package xyz.ksharma.krail.screenshot

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnimationScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `capture animation at different timestamps`() {
        val animationTimestamps = listOf(0L, 100L, 250L, 500L, 1000L)
        
        animationTimestamps.forEach { timestamp ->
            composeTestRule.mainClock.autoAdvance = false
            
            composeTestRule.setContent {
                // Your animated composable with @ScreenshotTest annotation
                AnimatedLoadingPreview()
            }
            
            // Advance time to specific timestamp
            composeTestRule.mainClock.advanceTimeBy(timestamp)
            
            composeTestRule.onRoot().captureRoboImage(
                filePath = "screenshots/animation_loading_${timestamp}ms.png"
            )
        }
    }
}

// Example animated preview
@ScreenshotTest(description = "Loading animation")
@Preview
@Composable
private fun AnimatedLoadingPreview() {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        ) { value, _ ->
            progress = value
        }
    }
    
    PreviewTheme {
        CircularProgressIndicator(progress = progress)
    }
}

// Example with manual clock control
@ScreenshotTest(description = "Fade in animation")
@Preview
@Composable
private fun FadeInAnimationPreview() {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    PreviewTheme {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            MyComponent()
        }
    }
}
```

### Multi-Scale Testing (Automatic)

Update your test file to automatically test multiple font scales:

```kotlin
// composeApp/src/androidUnitTest/kotlin/xyz/ksharma/krail/screenshot/PreviewScreenshotTest.kt

@Test
fun generateAllScreenshotsWithMultipleScales() {
    screenshotPreviews.forEach { preview ->
        val screenshotConfig = preview.getAnnotation<ScreenshotTest>()
        
        // Test at different font scales
        val fontScales = listOf(1.0f, 1.5f, 2.0f)
        
        fontScales.forEach { fontScale ->
            val fileName = buildScreenshotFileName(preview, fontScale)
            
            composeTestRule.setContent {
                ProvidePreviewEnvironment(fontScale = fontScale) {
                    preview.invoke()
                }
            }

            composeTestRule.onRoot().captureRoboImage(
                filePath = "screenshots/$fileName.png",
                roborazziOptions = ScreenshotDefaults.defaultRoborazziOptions(
                    threshold = screenshotConfig?.threshold ?: 0.0
                )
            )
        }
    }
}

@Composable
private fun ProvidePreviewEnvironment(
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
        KrailTheme {
            content()
        }
    }
}

private fun buildScreenshotFileName(
    preview: ComposablePreview<AndroidPreviewInfo>,
    fontScale: Float
): String {
    val className = preview.declaringClass.substringAfterLast(".")
    val methodName = preview.methodName
    val previewName = preview.previewInfo.name.takeIf { it.isNotBlank() } ?: methodName
    val scaleText = when (fontScale) {
        1.0f -> "normal"
        1.5f -> "large"
        2.0f -> "xlarge"
        else -> "scale_${fontScale}x"
    }
    return "${className}_${previewName}_${scaleText}"
        .replace(" ", "_")
        .replace("[^a-zA-Z0-9_]".toRegex(), "")
}
```

### Conditional Configuration Based on Preview Properties

```kotlin
// Smart configuration that adapts based on preview characteristics
@Test
fun generateSmartScreenshots() {
    screenshotPreviews.forEach { preview ->
        val config = determineConfig(preview)
        
        config.fontScales.forEach { fontScale ->
            config.devices.forEach { device ->
                if (config.testDarkMode) {
                    // Test both light and dark
                    listOf(false, true).forEach { isDark ->
                        captureWithConfig(preview, fontScale, device, isDark, config)
                    }
                } else {
                    // Test only light mode
                    captureWithConfig(preview, fontScale, device, false, config)
                }
            }
        }
    }
}

private fun determineConfig(preview: ComposablePreview<AndroidPreviewInfo>): ScreenshotTestConfig {
    val annotation = preview.getAnnotation<ScreenshotTest>()
    
    // Check if preview name suggests specific configuration
    val previewName = preview.previewInfo.name.lowercase()
    
    return when {
        previewName.contains("tablet") -> ScreenshotTestConfig(
            devices = listOf(ScreenshotDefaults.Devices.TABLET_PORTRAIT)
        )
        previewName.contains("landscape") -> ScreenshotTestConfig(
            devices = listOf(ScreenshotDefaults.Devices.PHONE_LANDSCAPE)
        )
        previewName.contains("animation") -> ScreenshotTestConfig(
            animationTimestamps = listOf(0L, 100L, 500L),
            threshold = 0.01 // Animations may have slight variations
        )
        previewName.contains("dark") -> ScreenshotTestConfig(
            testDarkMode = true
        )
        else -> ScreenshotTestConfig() // Use defaults
    }
}
```

### Real-World Configuration Examples from KRAIL

```kotlin
// Transport Mode Icons - Test all scales
@ScreenshotTest(description = "Transport icons support accessibility")
@Preview(group = "Transport Mode Icons")
@Composable
private fun TransportIconsAccessibility() {
    // Will automatically test at 1.0f, 1.5f, 2.0f
    PreviewTheme {
        Row {
            TransportModeIcon(transportMode = TransportMode.Train())
            TransportModeIcon(transportMode = TransportMode.Bus())
            TransportModeIcon(transportMode = TransportMode.Ferry())
        }
    }
}

// Saved Trip Card - Test single scale only
@ScreenshotTest(description = "Trip card - normal scale only")
@Preview
@Composable
private fun SavedTripCardSingleScale() {
    PreviewTheme(fontScale = 1.0f) {  // Explicitly set to skip multi-scale
        SavedTripCard(
            trip = Trip(
                fromStopId = "1",
                fromStopName = "Edmondson Park Station",
                toStopId = "2",
                toStopName = "Harris Park Station",
            ),
            primaryTransportMode = TransportMode.Train(),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

// Search Results - Test with loading animation
@ScreenshotTest(
    threshold = 0.01,
    description = "Search with loading animation at 0ms, 300ms, 600ms"
)
@Preview
@Composable
private fun SearchWithLoadingAnimation() {
    PreviewTheme {
        SearchResultsWithLoader()
    }
}

// Park & Ride - Tablet landscape
@ScreenshotTest(description = "Park & Ride info on tablet")
@Preview(device = "spec:width=1920dp,height=1200dp,dpi=240,orientation=landscape")
@Composable
private fun ParkRideTabletLandscape() {
    PreviewTheme {
        ParkRideFacilityInfo(/* ... */)
    }
}
```

### Selective Scanning

```kotlin
// In your test file, you can further filter:
AndroidComposablePreviewScanner()
    .scanPackageTrees(
        include = listOf(
            "xyz.ksharma.krail.trip.planner",  // Only trip planner
            "xyz.ksharma.krail.park.ride"      // And park & ride
        )
    )
    .includeIfAnnotatedWithAnyOf(ScreenshotTest::class.java)
    .filterPreviews { preview ->
        // Additional custom filtering
        preview.previewInfo.name.contains("Important")
    }
    .getPreviews()
```

### Multiple Test Classes

```kotlin
// For better organization:

// Trip planner screenshots
class TripPlannerScreenshotTest {
    // Scan only trip planner package
}

// Park & Ride screenshots
class ParkRideScreenshotTest {
    // Scan only park & ride package
}

// Design system screenshots
class DesignSystemScreenshotTest {
    // Scan only taj components
}
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Screenshot Tests

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  screenshot-test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Verify Screenshots
        run: ./gradlew composeApp:verifyRoborazziDebug
      
      - name: Upload diff images on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: screenshot-diffs
          path: composeApp/build/outputs/roborazzi/**/*_compare.png
      
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: roborazzi-report
          path: composeApp/build/reports/roborazzi/
```

---

## Screenshot Storage & Git Management

### Where Screenshots Are Generated

Roborazzi generates screenshots in different locations based on the command:

```bash
# Default location (configurable)
composeApp/build/outputs/roborazzi/
└── screenshots/
    ├── SavedTripCard_Preview_normal.png
    ├── SavedTripCard_Preview_large.png
    ├── SavedTripCard_Preview_xlarge.png
    ├── TransportModeIcon_Train_normal.png
    └── ...

# When verification fails, diff images are generated
composeApp/build/outputs/roborazzi/
└── screenshots/
    ├── SavedTripCard_Preview_normal.png           # Original
    ├── SavedTripCard_Preview_normal_compare.png   # Diff image (red/green overlay)
    └── SavedTripCard_Preview_normal_actual.png    # Current failed attempt
```

### Custom Screenshot Location (Recommended for Git)

Configure a custom directory that's easier to track:

```kotlin
// In your test file
composeTestRule.onRoot().captureRoboImage(
    filePath = "src/test/screenshots/$fileName.png"  // Custom path
)
```

Or configure via `roborazzi` block in `composeApp/build.gradle.kts`:

```kotlin
roborazzi {
    outputDir = "src/test/screenshots"  // Custom directory
}
```

**Recommended structure:**

```
KRAIL/
├── composeApp/
│   ├── screenshots/                     # ← Commit to Git with LFS
│   │   ├── transport/
│   │   │   ├── TransportModeIcon_Train_normal.png
│   │   │   ├── TransportModeIcon_Train_large.png
│   │   │   └── TransportModeIcon_Train_xlarge.png
│   │   ├── trip-planner/
│   │   │   ├── SavedTripCard_Preview_normal.png
│   │   │   └── SavedTripCard_Preview_large.png
│   │   └── park-ride/
│   │       └── ParkRideFacility_Preview_normal.png
│   │
│   └── build/
│       └── outputs/
│           └── roborazzi/              # ← Gitignore (temporary files)
│               ├── *_compare.png       # Diff images
│               └── *_actual.png        # Failed attempts
```

### Git LFS Setup (For Large Screenshot Files)

#### 1. Install Git LFS

```bash
# macOS
brew install git-lfs

# Or download from https://git-lfs.github.com/
```

#### 2. Initialize Git LFS in Your Repo

```bash
cd /Users/ksharma/code/apps/KRAIL
git lfs install
```

#### 3. Create `.gitattributes` File

Create: `/Users/ksharma/code/apps/KRAIL/.gitattributes`

```bash
# Track PNG screenshot files with Git LFS
composeApp/screenshots/**/*.png filter=lfs diff=lfs merge=lfs -text
composeApp/build/outputs/roborazzi/**/*.png filter=lfs diff=lfs merge=lfs -text

# If you also store screenshots in modules
**/screenshots/**/*.png filter=lfs diff=lfs merge=lfs -text

# Optional: Track other large binary files
*.mp4 filter=lfs diff=lfs merge=lfs -text
*.zip filter=lfs diff=lfs merge=lfs -text
```

#### 4. Update `.gitignore`

Create/update: `/Users/ksharma/code/apps/KRAIL/.gitignore`

```bash
# Build outputs (don't commit)
**/build/
**/outputs/

# Roborazzi temporary files (don't commit)
**/*_compare.png
**/*_actual.png

# Keep master screenshots (commit with LFS)
!composeApp/screenshots/**/*.png

# Optional: HTML reports from Roborazzi
**/reports/roborazzi/
```

#### 5. Track Existing Files

```bash
# If you have existing screenshots, track them with LFS
git lfs track "composeApp/screenshots/**/*.png"

# Check what's being tracked
git lfs track

# See LFS file list
git lfs ls-files
```

### Commit Workflow with Screenshots

#### Initial Setup (One-Time)

```bash
# 1. Setup Git LFS
git lfs install
git lfs track "composeApp/screenshots/**/*.png"

# 2. Commit .gitattributes
git add .gitattributes
git commit -m "chore: setup Git LFS for screenshots"

# 3. Generate initial screenshots
./gradlew composeApp:recordRoborazziDebug

# 4. Commit baseline screenshots
git add composeApp/screenshots/
git commit -m "test: add baseline screenshots"
git push
```

#### Regular Workflow (When UI Changes)

```bash
# 1. Make UI changes in your code
# Edit: feature/trip-planner/ui/.../SavedTripCard.kt

# 2. Run verification to see what broke
./gradlew composeApp:verifyRoborazziDebug

# If tests fail:
# 3. Review diff images
open composeApp/build/outputs/roborazzi/screenshots/*_compare.png

# 4. If changes are intentional, update screenshots
./gradlew composeApp:recordRoborazziDebug

# 5. Verify the update
./gradlew composeApp:verifyRoborazziDebug

# 6. Commit updated screenshots
git add composeApp/screenshots/
git commit -m "test: update SavedTripCard screenshots after UI change"
git push
```

#### Pull Request Workflow

```bash
# 1. Create feature branch
git checkout -b feature/update-transport-icons

# 2. Make changes and update screenshots
./gradlew composeApp:recordRoborazziDebug

# 3. Commit code + screenshots together
git add feature/trip-planner/ui/
git add composeApp/screenshots/transport/
git commit -m "feat: update transport mode icons

- Changed icon size from 28dp to 32dp
- Updated color scheme
- Screenshots updated for all scales"

# 4. Push (LFS will handle large files)
git push origin feature/update-transport-icons

# 5. CI will verify screenshots match
# GitHub Actions runs: ./gradlew composeApp:verifyRoborazziDebug
```

### Organizing Screenshots by Feature

```kotlin
// Update test file to organize by feature/module
private fun buildScreenshotPath(preview: ComposablePreview<AndroidPreviewInfo>): String {
    val packagePath = preview.declaringClass
        .substringBeforeLast(".")
        .replace("xyz.ksharma.krail.", "")
        .replace(".", "/")
    
    val fileName = buildScreenshotFileName(preview, fontScale)
    
    return "composeApp/screenshots/$packagePath/$fileName.png"
}

// Results in organized structure:
// composeApp/screenshots/
//   ├── trip/planner/ui/components/
//   │   ├── SavedTripCard_Preview_normal.png
//   │   └── SavedTripCard_Preview_large.png
//   ├── park/ride/ui/components/
//   │   └── ParkRideFacility_Preview_normal.png
//   └── taj/components/
//       ├── TextField_Preview_normal.png
//       └── Divider_Preview_normal.png
```

### File Structure

```
KRAIL/
├── .gitattributes                      # ← LFS configuration
├── .gitignore                          # ← Ignore build outputs
├── composeApp/
│   ├── build.gradle.kts                # ← Configure here
│   ├── screenshots/                    # ← COMMIT TO GIT (with LFS)
│   │   ├── trip/
│   │   │   └── planner/
│   │   │       └── ui/
│   │   │           ├── SavedTripCard_Preview_normal.png
│   │   │           ├── SavedTripCard_Preview_large.png
│   │   │           └── SavedTripCard_Preview_xlarge.png
│   │   ├── transport/
│   │   │   ├── TransportModeIcon_Train_normal.png
│   │   │   ├── TransportModeIcon_Bus_normal.png
│   │   │   └── ...
│   │   └── taj/
│   │       └── components/
│   │           ├── TextField_Preview_normal.png
│   │           └── Divider_Preview_normal.png
│   │
│   ├── src/
│   │   ├── commonMain/
│   │   │   └── kotlin/
│   │   │       └── xyz/ksharma/krail/
│   │   │           ├── feature/         # Your previews here! ✅
│   │   │           │   └── *.kt         # Add @ScreenshotTest
│   │   │           └── taj/             # Theme components
│   │   └── androidUnitTest/
│   │       └── kotlin/
│   │           └── xyz/ksharma/krail/
│   │               └── screenshot/
│   │                   ├── ScreenshotConfig.kt      # Config
│   │                   ├── PreviewScreenshotTest.kt # Main tests
│   │                   └── AnimationScreenshotTest.kt # Animation tests
│   │
│   └── build/
│       └── outputs/
│           └── roborazzi/              # ← GITIGNORE (temporary)
│               └── screenshots/
│                   ├── *_compare.png   # Diff images
│                   └── *_actual.png    # Failed attempts
│
├── feature/
│   ├── trip-planner/
│   │   └── ui/
│   │       └── screenshots/            # ← Optional: module-level screenshots
│   └── park-ride/
│       └── ui/
│           └── screenshots/
│
└── taj/
    └── src/
        └── commonMain/
            └── kotlin/
                └── xyz/ksharma/krail/taj/
                    └── screenshot/
                        └── ScreenshotTest.kt  # ← Custom annotation
```

### Checking LFS Status

```bash
# Check what files are tracked by LFS
git lfs ls-files

# Check LFS storage usage
git lfs status

# See LFS file info
git lfs ls-files --size

# Verify file is stored in LFS
git lfs ls-files | grep "SavedTripCard"

# Fetch all LFS objects
git lfs fetch --all

# Pull LFS files
git lfs pull
```

---

## Code Quality & Monitoring

### Snapshot Quality Metrics Script

Create a script to monitor snapshot health and quality:

Create: `scripts/snapshot-stats.sh`

```bash
#!/bin/bash

# Snapshot Quality Metrics for KRAIL
# Usage: ./scripts/snapshot-stats.sh

set -e

SCREENSHOTS_DIR="composeApp/screenshots"
TEMP_DIR="composeApp/build/outputs/roborazzi/screenshots"

echo "================================================"
echo "📊 KRAIL Snapshot Testing Metrics"
echo "================================================"
echo ""

# Count total screenshots
total_screenshots=$(find "$SCREENSHOTS_DIR" -name "*.png" 2>/dev/null | wc -l | tr -d ' ')
echo "📸 Total Screenshots: $total_screenshots"

# Calculate total size
total_size=$(du -sh "$SCREENSHOTS_DIR" 2>/dev/null | cut -f1)
echo "💾 Total Size: $total_size"

# Calculate average size
if [ "$total_screenshots" -gt 0 ]; then
    total_bytes=$(du -s "$SCREENSHOTS_DIR" 2>/dev/null | cut -f1)
    avg_kb=$((total_bytes / total_screenshots))
    echo "📏 Average Size: ${avg_kb}KB per screenshot"
fi

echo ""
echo "================================================"
echo "📁 Screenshots by Feature"
echo "================================================"
echo ""

# Count by feature/module
for feature_dir in "$SCREENSHOTS_DIR"/*; do
    if [ -d "$feature_dir" ]; then
        feature_name=$(basename "$feature_dir")
        count=$(find "$feature_dir" -name "*.png" | wc -l | tr -d ' ')
        size=$(du -sh "$feature_dir" 2>/dev/null | cut -f1)
        echo "  $feature_name: $count screenshots ($size)"
    fi
done

echo ""
echo "================================================"
echo "🔍 Screenshot Variants"
echo "================================================"
echo ""

# Count by variant (normal, large, xlarge)
normal_count=$(find "$SCREENSHOTS_DIR" -name "*_normal.png" 2>/dev/null | wc -l | tr -d ' ')
large_count=$(find "$SCREENSHOTS_DIR" -name "*_large.png" 2>/dev/null | wc -l | tr -d ' ')
xlarge_count=$(find "$SCREENSHOTS_DIR" -name "*_xlarge.png" 2>/dev/null | wc -l | tr -d ' ')
landscape_count=$(find "$SCREENSHOTS_DIR" -name "*_landscape*.png" 2>/dev/null | wc -l | tr -d ' ')
tablet_count=$(find "$SCREENSHOTS_DIR" -name "*_tablet*.png" 2>/dev/null | wc -l | tr -d ' ')
dark_count=$(find "$SCREENSHOTS_DIR" -name "*_dark*.png" 2>/dev/null | wc -l | tr -d ' ')

echo "  Normal (1.0x): $normal_count"
echo "  Large (1.5x): $large_count"
echo "  XLarge (2.0x): $xlarge_count"
echo "  Landscape: $landscape_count"
echo "  Tablet: $tablet_count"
echo "  Dark Mode: $dark_count"

echo ""
echo "================================================"
echo "⚠️  Large Screenshots (>200KB)"
echo "================================================"
echo ""

# Find large screenshots
large_files=$(find "$SCREENSHOTS_DIR" -name "*.png" -size +200k 2>/dev/null)
if [ -z "$large_files" ]; then
    echo "  ✅ No large screenshots found"
else
    echo "$large_files" | while read -r file; do
        size=$(du -h "$file" | cut -f1)
        filename=$(basename "$file")
        echo "  ⚠️  $filename: $size"
    done
fi

echo ""
echo "================================================"
echo "🔄 Recent Changes (Last 7 days)"
echo "================================================"
echo ""

# Find recently modified screenshots
recent=$(find "$SCREENSHOTS_DIR" -name "*.png" -mtime -7 2>/dev/null | wc -l | tr -d ' ')
echo "  $recent screenshots modified in last 7 days"

if [ "$recent" -gt 0 ]; then
    echo ""
    find "$SCREENSHOTS_DIR" -name "*.png" -mtime -7 2>/dev/null | while read -r file; do
        filename=$(basename "$file")
        mod_time=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M" "$file" 2>/dev/null || stat -c "%y" "$file" 2>/dev/null | cut -d'.' -f1)
        echo "    $filename - $mod_time"
    done
fi

echo ""
echo "================================================"
echo "🎯 Coverage Metrics"
echo "================================================"
echo ""

# Count @Preview annotations in commonMain
total_previews=$(find composeApp/src/commonMain -name "*.kt" -type f -exec grep -l "@Preview" {} \; 2>/dev/null | wc -l | tr -d ' ')
echo "  Total @Preview composables: $total_previews"

# Count @ScreenshotTest annotations
screenshot_tests=$(find composeApp/src/commonMain -name "*.kt" -type f -exec grep -l "@ScreenshotTest" {} \; 2>/dev/null | wc -l | tr -d ' ')
echo "  @ScreenshotTest annotated: $screenshot_tests"

if [ "$total_previews" -gt 0 ]; then
    coverage=$((screenshot_tests * 100 / total_previews))
    echo "  Coverage: ${coverage}%"
fi

echo ""
echo "================================================"
echo "✅ Quality Summary"
echo "================================================"
echo ""

# Calculate health score
health_score=100

# Deduct points for large files
if [ -n "$large_files" ]; then
    large_count=$(echo "$large_files" | wc -l | tr -d ' ')
    health_score=$((health_score - large_count * 2))
fi

# Deduct points for low coverage
if [ "$total_previews" -gt 0 ]; then
    coverage_percent=$((screenshot_tests * 100 / total_previews))
    if [ "$coverage_percent" -lt 50 ]; then
        health_score=$((health_score - 10))
    fi
fi

# Display health score
if [ "$health_score" -ge 90 ]; then
    echo "  🟢 Health Score: $health_score/100 (Excellent)"
elif [ "$health_score" -ge 70 ]; then
    echo "  🟡 Health Score: $health_score/100 (Good)"
else
    echo "  🔴 Health Score: $health_score/100 (Needs Attention)"
fi

echo ""
echo "================================================"
echo ""
```

Make it executable:

```bash
chmod +x scripts/snapshot-stats.sh
```

Run it:

```bash
./scripts/snapshot-stats.sh
```

### Gradle Task for Stats

Add to `composeApp/build.gradle.kts`:

```kotlin
tasks.register("screenshotStats") {
    group = "verification"
    description = "Display snapshot testing metrics and statistics"
    
    doLast {
        val screenshotsDir = file("screenshots")
        
        if (!screenshotsDir.exists()) {
            println("❌ No screenshots directory found")
            return@doLast
        }
        
        val screenshots = screenshotsDir.walk()
            .filter { it.isFile && it.extension == "png" }
            .toList()
        
        println("📊 Snapshot Statistics")
        println("=" .repeat(50))
        println("Total screenshots: ${screenshots.size}")
        
        val totalSize = screenshots.sumOf { it.length() }
        val avgSize = if (screenshots.isNotEmpty()) totalSize / screenshots.size else 0
        
        println("Total size: ${totalSize / 1024 / 1024}MB")
        println("Average size: ${avgSize / 1024}KB")
        
        // Group by directory
        val byFeature = screenshots.groupBy { 
            it.parentFile.name 
        }
        
        println("\nBy Feature:")
        byFeature.forEach { (feature, files) ->
            println("  $feature: ${files.size} screenshots")
        }
        
        // Find large files
        val largeFiles = screenshots.filter { it.length() > 200 * 1024 }
        if (largeFiles.isNotEmpty()) {
            println("\n⚠️  Large files (>200KB):")
            largeFiles.forEach {
                println("  ${it.name}: ${it.length() / 1024}KB")
            }
        }
    }
}
```

Run with:

```bash
./gradlew composeApp:screenshotStats
```

### GitHub Actions - Size Check

Add to your CI workflow:

```yaml
# .github/workflows/screenshot-quality.yml
name: Screenshot Quality Check

on:
  pull_request:
    paths:
      - 'composeApp/screenshots/**'

jobs:
  check-size:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          lfs: true
      
      - name: Check screenshot sizes
        run: |
          echo "Checking screenshot sizes..."
          
          # Find large screenshots (>300KB)
          large_files=$(find composeApp/screenshots -name "*.png" -size +300k)
          
          if [ -n "$large_files" ]; then
            echo "❌ Found large screenshots (>300KB):"
            echo "$large_files" | while read file; do
              size=$(du -h "$file" | cut -f1)
              echo "  - $file: $size"
            done
            echo ""
            echo "💡 Consider:"
            echo "  1. Reducing preview size"
            echo "  2. Using recordOptions.resizeScale = 0.5"
            echo "  3. Optimizing PNG files"
            exit 1
          else
            echo "✅ All screenshots are reasonably sized"
          fi
      
      - name: Check total size
        run: |
          total_size=$(du -sh composeApp/screenshots | cut -f1)
          echo "📦 Total screenshot size: $total_size"
          
          # Check if over 50MB
          size_bytes=$(du -s composeApp/screenshots | cut -f1)
          if [ "$size_bytes" -gt 51200 ]; then  # 50MB in KB
            echo "⚠️  Screenshot directory is getting large (>50MB)"
            echo "💡 Consider using Git LFS or optimizing images"
          fi
```

### Future TODOs & Improvements

Create: `docs/SNAPSHOT_TESTING_TODOS.md`

```markdown
# Snapshot Testing - Future Improvements

## Priority 1: Setup & Foundation ✅ (Current)
- [x] Choose testing library (Roborazzi + ComposablePreviewScanner)
- [x] Add dependencies and configure build files
- [x] Create @ScreenshotTest annotation
- [x] Write base test file with scanning logic
- [x] Setup Git LFS for screenshot storage
- [x] Document usage and workflow

## Priority 2: Coverage & Quality (Next Sprint)
- [ ] **Annotate critical components** (Target: 50% coverage)
  - [ ] Trip Planner components (SavedTripCard, SearchStopRow, etc.)
  - [ ] Transport mode icons
  - [ ] Park & Ride components
  - [ ] Design system (Taj) components
- [ ] **Run initial baseline generation**
  - [ ] `./gradlew composeApp:recordRoborazziDebug`
  - [ ] Review all generated screenshots
  - [ ] Commit to git with LFS
- [ ] **Setup CI/CD integration**
  - [ ] Add GitHub Actions workflow for verification
  - [ ] Configure PR checks
  - [ ] Setup artifact upload for failed tests

## Priority 3: Optimization (Month 2)
- [ ] **File Size Optimization**
  - [ ] Review current average screenshot size
  - [ ] Target: <150KB per screenshot
  - [ ] Investigate PNG compression tools
  - [ ] Consider using `resizeScale = 0.8` for non-critical tests
  - [ ] Add automated size checks in CI
- [ ] **Organization & Structure**
  - [ ] Organize screenshots by feature module
  - [ ] Create naming conventions document
  - [ ] Setup directory structure: screenshots/{feature}/{component}/
  - [ ] Add screenshot count limits per feature
- [ ] **Performance**
  - [ ] Measure test execution time
  - [ ] Target: <5 minutes for all screenshot tests
  - [ ] Parallelize test execution if needed
  - [ ] Cache Robolectric dependencies in CI

## Priority 4: Advanced Features (Month 3)
- [ ] **Multi-Device Testing**
  - [ ] Add tablet configurations for adaptive layouts
  - [ ] Test landscape orientation for critical screens
  - [ ] Add foldable device configurations
  - [ ] Document when to use each device config
- [ ] **Accessibility Testing**
  - [ ] Test all font scales (1.0f, 1.5f, 2.0f) automatically
  - [ ] Add high contrast mode tests
  - [ ] Test with different system locales
  - [ ] Verify minimum touch target sizes
- [ ] **Animation Testing**
  - [ ] Identify components with animations
  - [ ] Setup frame-by-frame capture
  - [ ] Test loading states
  - [ ] Test transitions and state changes
- [ ] **Dark Mode Coverage**
  - [ ] Identify components needing dark mode tests
  - [ ] Add dark mode variants for critical components
  - [ ] Setup automatic light/dark comparison

## Priority 5: Monitoring & Metrics (Ongoing)
- [ ] **Quality Metrics Dashboard**
  - [ ] Track total screenshot count over time
  - [ ] Monitor average file size trends
  - [ ] Track test execution time
  - [ ] Monitor storage usage (Git LFS)
  - [ ] Track coverage percentage
- [ ] **Automated Reporting**
  - [ ] Weekly snapshot health report
  - [ ] PR comment with screenshot changes
  - [ ] Slack/Discord notifications for failures
  - [ ] Monthly coverage report
- [ ] **Cost Monitoring**
  - [ ] Track Git LFS bandwidth usage
  - [ ] Monitor CI/CD minutes used
  - [ ] Estimate storage costs
  - [ ] Setup alerts for unusual growth

## Code Quality Checks

### Size Monitoring
```bash
# Track screenshot sizes
find composeApp/screenshots -name "*.png" -exec du -h {} \; | sort -rh | head -20

# Get total size
du -sh composeApp/screenshots

# Average size
total_bytes=$(du -s composeApp/screenshots | cut -f1)
count=$(find composeApp/screenshots -name "*.png" | wc -l)
echo "Average: $((total_bytes / count))KB"
```

### Count Tracking
```bash
# Total screenshots
find composeApp/screenshots -name "*.png" | wc -l

# By feature
for dir in composeApp/screenshots/*/; do
  echo "$(basename $dir): $(find $dir -name '*.png' | wc -l)"
done

# By variant
echo "Normal: $(find composeApp/screenshots -name '*_normal.png' | wc -l)"
echo "Large: $(find composeApp/screenshots -name '*_large.png' | wc -l)"
echo "XLarge: $(find composeApp/screenshots -name '*_xlarge.png' | wc -l)"
```

### Coverage Tracking
```bash
# Total @Preview annotations
total_previews=$(grep -r "@Preview" composeApp/src/commonMain --include="*.kt" | wc -l)

# @ScreenshotTest annotations
screenshot_tests=$(grep -r "@ScreenshotTest" composeApp/src/commonMain --include="*.kt" | wc -l)

# Coverage percentage
echo "Coverage: $((screenshot_tests * 100 / total_previews))%"
```

### Quality Thresholds
- **Max screenshot size:** 200KB
- **Total directory size:** <100MB (without LFS)
- **Average screenshot size:** <150KB
- **Test execution time:** <10 minutes
- **Coverage target:** 70% of @Preview composables
- **Max screenshots per preview:** 6 (3 scales × 2 themes)

## Maintenance Tasks

### Weekly
- [ ] Review failed screenshot tests in CI
- [ ] Check for new large screenshots (>200KB)
- [ ] Update screenshots for merged UI changes
- [ ] Review snapshot-stats.sh output

### Monthly
- [ ] Run full screenshot audit
- [ ] Review storage usage (Git LFS)
- [ ] Check coverage metrics
- [ ] Update documentation
- [ ] Clean up unused screenshots
- [ ] Optimize large screenshots

### Quarterly
- [ ] Review testing strategy effectiveness
- [ ] Update dependencies (Roborazzi, scanner)
- [ ] Evaluate new features from libraries
- [ ] Performance optimization review
- [ ] Team training/workshop on screenshot testing

## Investigation Tasks

### When to Investigate
- Total size >100MB
- Average size >200KB
- Test execution >15 minutes
- Coverage <50%
- Frequent false positives

### Investigation Steps
1. Run `./scripts/snapshot-stats.sh`
2. Identify outliers (large files, slow tests)
3. Review recent changes
4. Check CI logs for patterns
5. Document findings
6. Create improvement tasks

## Future Enhancements

### Tool Improvements
- [ ] Create IntelliJ/Android Studio plugin for easier annotation
- [ ] Build Gradle plugin to automate common tasks
- [ ] Create web dashboard for screenshot history
- [ ] Setup visual regression diff viewer
- [ ] Integrate with design tools (Figma)

### Process Improvements
- [ ] Add screenshot review checklist to PR template
- [ ] Create screenshot testing best practices guide
- [ ] Setup mentoring/pairing for new team members
- [ ] Create video tutorials
- [ ] Document common issues and solutions

### Integration Ideas
- [ ] Integrate with Jira/Linear for design QA
- [ ] Connect to Figma for design comparisons
- [ ] Add Slack bot for screenshot updates
- [ ] Create email digest of weekly changes
- [ ] Setup A/B testing with screenshots
```

### Quick Health Check Commands

Add these aliases to your `.zshrc` or `.bashrc`:

```bash
# Add to ~/.zshrc
alias krail-screenshot-stats="cd /Users/ksharma/code/apps/KRAIL && ./scripts/snapshot-stats.sh"
alias krail-screenshot-size="cd /Users/ksharma/code/apps/KRAIL && du -sh composeApp/screenshots"
alias krail-screenshot-count="cd /Users/ksharma/code/apps/KRAIL && find composeApp/screenshots -name '*.png' | wc -l"
alias krail-screenshot-large="cd /Users/ksharma/code/apps/KRAIL && find composeApp/screenshots -name '*.png' -size +200k"
alias krail-screenshot-record="cd /Users/ksharma/code/apps/KRAIL && ./gradlew composeApp:recordRoborazziDebug"
alias krail-screenshot-verify="cd /Users/ksharma/code/apps/KRAIL && ./gradlew composeApp:verifyRoborazziDebug"
```

Then use:

```bash
krail-screenshot-stats    # Full report
krail-screenshot-size     # Quick size check
krail-screenshot-count    # Count screenshots
krail-screenshot-large    # Find large files
```

### ✅ Advantages

1. **Selective Screenshots**: Only annotated previews
2. **No Code Duplication**: Uses existing commonMain previews
3. **Flexible Configuration**: Per-preview thresholds
4. **Production Ready**: 150k+ monthly downloads
5. **Great Community**: Active support and updates
6. **Powerful Scanning**: Filter by package, name, annotations
7. **Rich Comparison**: Threshold, pixel-diff, regions
8. **CI-Friendly**: Easy GitHub Actions integration

### ⚠️ Limitations

1. **JVM Only**: Roborazzi runs on JVM, not actual iOS devices
2. **Setup Complexity**: More initial setup than Google's tool
3. **Learning Curve**: Need to understand scanning & filtering
4. **Robolectric Dependency**: Requires Robolectric for Android rendering
5. **File Management**: Need to commit screenshots to git (can use Git LFS)

---

## Alternative: Google Compose Screenshot Testing

### Quick Setup

```toml
[versions]
composeScreenshot = "0.0.1-alpha09"

[plugins]
screenshot = { id = "com.android.compose.screenshot", version.ref = "composeScreenshot" }
```

```kotlin
// composeApp/build.gradle.kts
plugins {
    alias(libs.plugins.screenshot)
}

android {
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}
```

```bash
./gradlew updateDebugScreenshotTest    # Record
./gradlew validateDebugScreenshotTest  # Verify
```

### ❌ Why Not Recommended for Your Requirements

1. **No Selective Annotation**: All previews or none
2. **Separate Source Set**: Must create `screenshotTest/` source set
3. **Code Duplication**: Can't use commonMain previews directly
4. **Experimental**: Alpha stage, API may change
5. **Limited Config**: Less control over comparison
6. **Uncertain Future**: Google's experimental projects sometimes get abandoned

### Example (Google Approach)

```kotlin
// Must create NEW file in screenshotTest/ ❌
// composeApp/src/screenshotTest/kotlin/Previews.kt

@Preview
@Composable
fun SavedTripCardPreview() {  // Duplicate from commonMain ❌
    PreviewTheme {
        SavedTripCard(/* ... */)
    }
}
```

---

## Recommendation: Use Roborazzi + ComposablePreviewScanner

### Why?

✅ **Perfectly matches your requirements:**
- ✅ Custom `@ScreenshotTest` annotation for selection
- ✅ Scans commonMain previews directly
- ✅ No code duplication
- ✅ Production-ready and stable

✅ **Better long-term:**
- Active maintenance
- Large community
- Flexible and powerful
- Used by major projects

### Migration Path

1. **Week 1**: Setup infrastructure (30 min)
2. **Week 2**: Annotate critical previews (1-2 hours)
3. **Week 3**: Record baseline screenshots (30 min)
4. **Week 4**: Add to CI/CD pipeline (1 hour)

### Total Investment: ~4-5 hours for production-ready snapshot testing

---

## Quick Start Checklist

- [ ] Add dependencies to `libs.versions.toml`
- [ ] Add plugin to root `build.gradle.kts`
- [ ] Configure `composeApp/build.gradle.kts`
- [ ] Create `@ScreenshotTest` annotation in taj module
- [ ] Create test file in `androidUnitTest/`
- [ ] Annotate 2-3 previews with `@ScreenshotTest`
- [ ] Run `./gradlew composeApp:recordRoborazziDebug`
- [ ] Verify output in `build/outputs/roborazzi/`
- [ ] Commit screenshots to git
- [ ] Add CI/CD validation

---

## Questions & Troubleshooting

### Q: Can I test iOS-specific UI?
**A:** Not directly with Roborazzi (JVM only). For iOS, consider:
- XCUITest snapshots (native iOS testing)
- Compose Multiplatform previews work, but rendered on JVM

### Q: How big are screenshot files?
**A:** Typically 50-200 KB per PNG. Use Git LFS if repo gets large.

### Q: Can I run tests locally before CI?
**A:** Yes! `./gradlew composeApp:verifyRoborazziDebug`

### Q: What if I change font or colors slightly?
**A:** Use threshold: `@ScreenshotTest(threshold = 0.01)` for 1% tolerance

### Q: Can I test dark mode?
**A:** Yes! Your `PreviewTheme(darkTheme = true)` already supports it

---

## Next Steps

1. **Ask questions** about this approach
2. **Review examples** with your team
3. **Decide** if this meets your needs
4. **I'll implement** the setup for you
5. **You annotate** previews and generate first screenshots

Ready to proceed? 🚀

---

## Quick Reference

### Essential Commands

```bash
# Record/Update Screenshots (after UI changes)
./gradlew composeApp:recordRoborazziDebug

# Verify Screenshots (before commit)
./gradlew composeApp:verifyRoborazziDebug

# Compare and Generate Report
./gradlew composeApp:compareRoborazziDebug

# View Statistics
./scripts/snapshot-stats.sh
./gradlew composeApp:screenshotStats

# Git LFS Commands
git lfs track "composeApp/screenshots/**/*.png"
git lfs ls-files
git lfs status
git lfs pull
```

### File Paths Quick Reference

```
Configuration:
  gradle/libs.versions.toml          - Dependencies
  build.gradle.kts (root)           - Plugin registration
  composeApp/build.gradle.kts       - Plugin configuration

Source Code:
  taj/.../screenshot/ScreenshotTest.kt              - Custom annotation
  composeApp/.../screenshot/ScreenshotConfig.kt     - Default configs
  composeApp/.../screenshot/PreviewScreenshotTest.kt - Main tests
  composeApp/.../screenshot/AnimationScreenshotTest.kt - Animation tests

Screenshots:
  composeApp/screenshots/           - COMMIT TO GIT (with LFS)
  composeApp/build/outputs/roborazzi/ - GITIGNORE (temporary)

Git:
  .gitattributes                    - LFS tracking config
  .gitignore                        - Ignore patterns

Documentation:
  docs/SNAPSHOT_TESTING_COMPARISON.md - This file
  docs/SNAPSHOT_TESTING_TODOS.md     - Future improvements
  scripts/snapshot-stats.sh          - Monitoring script
```

### Annotation Examples Cheat Sheet

```kotlin
// 1. Basic screenshot test
@ScreenshotTest
@Preview
@Composable
fun MyComponentPreview() { ... }

// 2. With custom threshold (allows 1% difference)
@ScreenshotTest(threshold = 0.01)
@Preview
@Composable
fun FlexiblePreview() { ... }

// 3. With description
@ScreenshotTest(description = "Loading state with animation")
@Preview
@Composable
fun DocumentedPreview() { ... }

// 4. Dark mode
@ScreenshotTest
@Preview
@Composable
fun DarkModePreview() {
    PreviewTheme(darkTheme = true) { ... }
}

// 5. Custom font scale (skip multi-scale)
@ScreenshotTest
@Preview
@Composable
fun SingleScalePreview() {
    PreviewTheme(fontScale = 1.0f) { ... }
}

// 6. Tablet landscape
@ScreenshotTest
@Preview(device = "spec:width=1920dp,height=1200dp,dpi=240,orientation=landscape")
@Composable
fun TabletPreview() { ... }

// 7. Multiple variants (multi-preview)
@Preview(name = "Light")
@Preview(name = "Dark")
@ScreenshotTest
@Composable
fun BothModesPreview() { ... }
```

### Configuration Snippets

```kotlin
// Default font scales (in ScreenshotConfig.kt)
val fontScales = listOf(1.0f, 1.5f, 2.0f)

// Device configurations
object Devices {
    const val PHONE_PORTRAIT = RobolectricDeviceQualifiers.Pixel6
    const val PHONE_LANDSCAPE = RobolectricDeviceQualifiers.Pixel6 + "-land"
    const val TABLET_PORTRAIT = RobolectricDeviceQualifiers.NexusTablet
    const val TABLET_LANDSCAPE = RobolectricDeviceQualifiers.NexusTablet + "-land"
}

// Roborazzi options
fun defaultRoborazziOptions(threshold: Double = 0.0) = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(
        resultValidator = ThresholdValidator(threshold)
    ),
    recordOptions = RoborazziOptions.RecordOptions(
        resizeScale = 1.0  // 1.0 = full size, 0.5 = half size
    )
)
```

### Git Workflow Quick Guide

```bash
# 1. Update screenshots after UI change
./gradlew composeApp:recordRoborazziDebug

# 2. Review changes
git diff composeApp/screenshots/

# 3. Stage and commit
git add composeApp/screenshots/
git commit -m "test: update screenshots for SavedTripCard"

# 4. Push (LFS handles large files)
git push

# 5. CI verifies screenshots match
# GitHub Actions: ./gradlew composeApp:verifyRoborazziDebug
```

### Troubleshooting Quick Fixes

| Issue | Solution |
|-------|----------|
| Screenshots not generated | Check `@ScreenshotTest` annotation present |
| File not found errors | Verify package name in `scanPackageTrees()` |
| Large file sizes (>200KB) | Use `resizeScale = 0.8` in RoborazziOptions |
| Tests fail randomly | Increase threshold: `@ScreenshotTest(threshold = 0.01)` |
| Slow test execution | Reduce font scale variants or parallelize tests |
| Git LFS bandwidth exceeded | Use `git lfs pull --exclude=""` to skip some files |
| CI fails but local passes | Ensure same SDK version in test and CI |
| Animation screenshots blank | Use `mainClock.autoAdvance = false` |

### Quality Thresholds

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Screenshot size | <150KB | 150-200KB | >200KB |
| Total directory size | <50MB | 50-100MB | >100MB |
| Test execution time | <5min | 5-10min | >10min |
| Coverage | >70% | 50-70% | <50% |
| Failed tests | 0 | 1-3 | >3 |

### Support & Resources

- **Main Documentation:** `docs/SNAPSHOT_TESTING_COMPARISON.md`
- **TODO List:** `docs/SNAPSHOT_TESTING_TODOS.md`
- **Stats Script:** `scripts/snapshot-stats.sh`
- **Roborazzi Docs:** https://github.com/takahirom/roborazzi
- **Scanner Docs:** https://github.com/sergio-sastre/ComposablePreviewScanner
- **Team Channel:** #krail-snapshot-testing (create if needed)

