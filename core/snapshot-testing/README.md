# Core: Snapshot Testing

Reusable snapshot testing infrastructure for KRAIL modules.

## Overview

This module provides the base configuration, annotations, and utilities for snapshot testing across all KRAIL modules. It uses:
- **Roborazzi** for JVM-based screenshot generation
- **ComposablePreviewScanner** for scanning `@Preview` annotations
- **Robolectric** for Android rendering
- **BaseSnapshotTest** for easy test creation

## Features

- ✅ Custom `@ScreenshotTest` annotation for selective screenshot generation
- ✅ `BaseSnapshotTest` class - just extend and override one property!
- ✅ Default configuration for consistent testing (font scales, dark mode, etc.)
- ✅ Multi-font-scale testing (1.0f, 2.0f) for accessibility
- ✅ Automatic dark mode testing
- ✅ Flexible configuration per-preview or per-module

## Quick Start for New Modules

### 1. Add Dependencies

In your module's `build.gradle.kts`:

```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.roborazzi)
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    sourceSets {
        val androidUnitTest by getting {
            dependencies {
                implementation(projects.core.snapshotTesting)
            }
        }
    }
}
```

### 2. Annotate Your Previews

In your `commonMain` or `androidMain` source set:

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.snapshot.ScreenshotTest

@ScreenshotTest
@Preview
@Composable
fun MyComponentPreview() {
    MyTheme {
        MyComponent()
    }
}
```

### 3. Create Test Class

In `yourmodule/src/androidUnitTest/kotlin/.../YourModuleSnapshotTest.kt`:

```kotlin
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE
)
class YourModuleSnapshotTest : BaseSnapshotTest() {
    
    // Just specify your package name!
    override val packageToScan = "com.example.yourmodule"
    
    @Test
    fun generateSnapshots() {
        generateSnapshots()
    }
}
```

That's it! Just 3 steps. 🎉

### 4. Run Tests

```bash
# Record screenshots
./gradlew :yourmodule:recordRoborazziDebug

# Verify screenshots (fails if different)
./gradlew :yourmodule:verifyRoborazziDebug

# Compare and generate report
./gradlew :yourmodule:compareRoborazziDebug
```

## Configuration

### Default Behavior

Each `@ScreenshotTest` preview automatically generates **3 screenshots**:
1. **Light mode, 1.0x** - Normal font scale
2. **Light mode, 2.0x** - Extra large font scale (accessibility)
3. **Dark mode, 1.0x** - Normal font scale

### BaseSnapshotTest Customization

Override properties to customize behavior:

```kotlin
class CustomSnapshotTest : BaseSnapshotTest() {
    override val packageToScan = "com.example.mymodule"
    
    // Custom screenshot directory (default: "screenshots")
    override val screenshotsDir = "custom-screenshots"
    
    // Include/exclude private previews (default: true)
    override val includePrivatePreviews = true
    
    // Custom light mode font scales (default: [1.0f, 2.0f])
    override val lightModeFontScales = listOf(1.0f, 1.5f, 2.0f)
    
    // Custom dark mode font scales (default: [1.0f])
    override val darkModeFontScales = listOf(1.0f)
    
    // Disable dark mode testing (default: true)
    override val testDarkMode = false
    
    @Test
    fun generateSnapshots() {
        generateSnapshots()
    }
}
```

### Per-Preview Configuration

Use annotation parameters:

```kotlin
// Custom threshold for components with animations or slight variations
@ScreenshotTest(threshold = 0.01)  // Allow 1% difference
@Preview
@Composable
fun AnimatedPreview() {
    MyTheme {
        AnimatedComponent()
    }
}

// With description for documentation
@ScreenshotTest(
    threshold = 0.02,
    description = "Loading spinner with pulsing animation"
)
@Preview
@Composable
fun LoadingPreview() {
    MyTheme {
        LoadingSpinner()
    }
}
```

### Advanced Preview Configurations

```kotlin
// Landscape orientation
@ScreenshotTest
@Preview(device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun LandscapePreview() {
    MyTheme {
        MyComponent()
    }
}

// Tablet
@ScreenshotTest
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun TabletPreview() {
    MyTheme {
        MyComponent()
    }
}

// Multiple previews for different states
@ScreenshotTest
@Preview(name = "Empty State")
@Composable
fun EmptyStatePreview() {
    MyTheme {
        MyComponent(items = emptyList())
    }
}

@ScreenshotTest
@Preview(name = "With Data")
@Composable
fun WithDataPreview() {
    MyTheme {
        MyComponent(items = previewData)
    }
}
```

## Default Settings (`SnapshotDefaults`)

Global defaults that apply to all tests:

- **Light Mode Font Scales:** `[1.0f, 2.0f]`
- **Dark Mode Font Scales:** `[1.0f]`
- **Device:** Pixel 6 portrait
- **SDK:** 34
- **Threshold:** 0.0 (exact match)
- **Test Dark Mode:** true

## File Structure

```
core/snapshot-testing/
├── src/
│   ├── commonMain/
│   │   └── kotlin/
│   │       └── xyz/ksharma/krail/core/snapshot/
│   │           └── ScreenshotTest.kt          # @ScreenshotTest annotation
│   └── androidMain/
│       └── kotlin/
│           └── xyz/ksharma/krail/core/snapshot/
│               ├── BaseSnapshotTest.kt        # Base test class
│               ├── SnapshotConfig.kt          # Default configuration
│               └── SnapshotTestHelper.kt      # Helper utilities (legacy)
└── build.gradle.kts
```

## API Reference

### `@ScreenshotTest`

Annotation to mark `@Preview` composables for screenshot testing.

**Parameters:**
- `threshold: Double = 0.0` - Comparison threshold (0.0 = exact match, 0.01 = 1% difference)
- `description: String = ""` - Optional description for documentation

### `BaseSnapshotTest`

Abstract base class for snapshot tests. Just extend and override `packageToScan`.

**Required:**
- `packageToScan: String` - Package to scan for previews

**Optional:**
- `screenshotsDir: String = "screenshots"` - Screenshot directory
- `includePrivatePreviews: Boolean = true` - Include private functions
- `lightModeFontScales: List<Float> = [1.0f, 2.0f]` - Light mode scales
- `darkModeFontScales: List<Float> = [1.0f]` - Dark mode scales
- `testDarkMode: Boolean = true` - Enable dark mode testing

**Methods:**
- `generateSnapshots()` - Call from your `@Test` method

### `SnapshotDefaults`

Configuration object with global defaults.

**Properties:**
- `lightModeFontScales: List<Float>`
- `darkModeFontScales: List<Float>`
- `defaultDevice: String`
- `testDarkMode: Boolean`
- `defaultThreshold: Double`
- `defaultSdk: Int`

**Methods:**
- `roborazziOptions(threshold: Double = 0.0, resizeScale: Double = 1.0): RoborazziOptions`

## Examples

See these files for complete examples:
- `taj/src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TajSnapshotTest.kt`
- `taj/src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TestPreviews.kt`

## Git LFS

Screenshots are tracked with Git LFS to keep repository size manageable. The `.gitattributes` file is already configured with:

```
**/screenshots/**/*.png filter=lfs diff=lfs merge=lfs -text
```

**Initial setup (one-time):**
```bash
# Install Git LFS
brew install git-lfs  # macOS
# or
apt-get install git-lfs  # Linux

# Initialize in repo
git lfs install

# Pull existing screenshots
git lfs pull
```

## Troubleshooting

### Screenshots not generated
- ✅ Verify `@ScreenshotTest` annotation is present on preview
- ✅ Check `packageToScan` matches your preview's package
- ✅ Ensure preview function is `@Composable` and has `@Preview`
- ✅ Run with `--rerun-tasks` to force execution

### Build error: "Unresolved reference: BaseSnapshotTest"
- ✅ Add `implementation(projects.core.snapshotTesting)` to `androidUnitTest` dependencies
- ✅ Sync Gradle
- ✅ Invalidate caches and restart IDE

### Wrong file path / nested directories
- ✅ Use `screenshotsDir = "screenshots"` (no module prefix)
- ✅ Screenshots save relative to module root
- ✅ Check for `taj/taj/screenshots` - remove duplicate directory

### Tests fail randomly
- ✅ Increase threshold: `@ScreenshotTest(threshold = 0.01)`
- ✅ Check for animations or dynamic content
- ✅ Ensure consistent environment (same SDK, device config)

### Large file sizes
- ✅ Keep preview sizes reasonable
- ✅ Use `roborazziOptions(resizeScale = 0.8)` for smaller files
- ✅ Verify Git LFS is installed and tracking PNGs

### Roborazzi task has no actions
- ✅ Test must be in `androidUnitTest` source set (not `test`)
- ✅ Run `testDebugUnitTest` task first to execute tests
- ✅ Then run `recordRoborazziDebug` or `verifyRoborazziDebug`

## CI/CD Integration

Snapshot tests run automatically in CI. Failed tests will:
1. Fail the build
2. Generate diff images
3. Upload artifacts for review
4. Comment on PR with visual changes

## Related Documentation

- [Taj Screenshots README](../../taj/screenshots/README.md) - Example implementation
- [Roborazzi Documentation](https://github.com/takahirom/roborazzi)
- [ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner)


## Configuration

### Default Settings (`SnapshotDefaults`)

- **Font Scales:** 1.0f, 1.5f, 2.0f (for accessibility testing)
- **Device:** Pixel 6 portrait
- **SDK:** 34
- **Threshold:** 0.0 (exact match)

### Per-Preview Override

```kotlin
// Single font scale only
@ScreenshotTest
@Preview
@Composable
fun SingleScalePreview() {
    PreviewTheme(fontScale = 1.0f) {  // Won't test other scales
        MyComponent()
    }
}

// Custom threshold for slight variations
@ScreenshotTest(threshold = 0.01)
@Preview
@Composable
fun FlexiblePreview() {
    PreviewTheme {
        ComponentWithAnimation()
    }
}

// Landscape mode
@ScreenshotTest
@Preview(device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun LandscapePreview() {
    PreviewTheme {
        MyComponent()
    }
}
```

## File Structure

```
core/snapshot-testing/
├── src/
│   ├── androidMain/
│   │   └── kotlin/
│   │       └── xyz/ksharma/krail/core/snapshot/
│   │           └── ScreenshotTest.kt          # Annotation
│   └── androidUnitTest/
│       └── kotlin/
│           └── xyz/ksharma/krail/core/snapshot/
│               ├── SnapshotConfig.kt          # Default configuration
│               └── SnapshotTestHelper.kt      # Helper utilities
└── build.gradle.kts
```

## API Reference

### `@ScreenshotTest`

Marks a `@Preview` composable for screenshot testing.

**Parameters:**
- `threshold: Double` - Comparison threshold (0.0 = exact match, 0.01 = 1% difference)
- `description: String` - Optional description for documentation

### `SnapshotDefaults`

Default configuration object.

**Properties:**
- `fontScales: List<Float>` - Default font scales to test
- `defaultDevice: String` - Default device configuration
- `defaultThreshold: Double` - Default comparison threshold
- `roborazziOptions()` - Creates Roborazzi options with defaults

### `SnapshotTestHelper`

Helper object with utilities.

**Methods:**
- `buildScreenshotFileName()` - Builds consistent file names
- `ProvidePreviewEnvironment()` - Composable wrapper for preview environment
- `captureScreenshot()` - Captures a single screenshot
- `captureWithFontScales()` - Captures at multiple font scales

## Examples

See `taj/src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TajSnapshotTest.kt` for a complete example.

## Git LFS

Screenshots are tracked with Git LFS to keep repository size manageable. The `.gitattributes` file is already configured.

**Initial setup:**
```bash
git lfs install
git lfs track "yourmodule/screenshots/**/*.png"
```

## Troubleshooting

### Screenshots not generated
- Verify `@ScreenshotTest` annotation is present
- Check package name in `scanPackageTrees()`
- Ensure test is in `androidUnitTest` source set

### Large file sizes
- Use `resizeScale = 0.8` in `roborazziOptions()`
- Keep preview sizes small
- Use `threshold > 0` for dynamic content

### Tests fail randomly
- Increase threshold: `@ScreenshotTest(threshold = 0.01)`
- Check for animations or dynamic content
- Ensure consistent environment (same SDK, device)

## Related Documentation

- [Main Snapshot Testing Guide](../../docs/SNAPSHOT_TESTING_COMPARISON.md)
- [TODO List](../../docs/SNAPSHOT_TESTING_TODOS.md)
- [Roborazzi Documentation](https://github.com/takahirom/roborazzi)
- [ComposablePreviewScanner Documentation](https://github.com/sergio-sastre/ComposablePreviewScanner)

