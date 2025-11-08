
# Snapshot Testing - Quick Reference

## TL;DR - For Feature Developers

### Add Snapshot Tests to Your Module (3 Steps)

#### 1. Update `build.gradle.kts`
```kotlin
plugins {
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

#### 2. Annotate Your Previews
```kotlin
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.snapshot.ScreenshotTest

@ScreenshotTest
@Preview
@Composable
fun MyComponentPreview() {
    MyComponent()
}
```

#### 3. Create Test Class
```kotlin
// In src/androidUnitTest/kotlin/...

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class MyModuleSnapshotTest : BaseSnapshotTest() {
    override val packageToScan = "com.example.mymodule"
    
    @Test
    fun generateSnapshots() {
        generateSnapshots()
    }
}
```

### Commands

```bash
# Record new/updated screenshots
./gradlew :yourmodule:recordRoborazziDebug

# Verify screenshots match
./gradlew :yourmodule:verifyRoborazziDebug

# Compare and see diff
./gradlew :yourmodule:compareRoborazziDebug
```

## What You Get

Each `@ScreenshotTest` preview automatically generates **3 screenshots**:
- ✅ Light mode @ 1.0x font scale
- ✅ Light mode @ 2.0x font scale (accessibility)
- ✅ Dark mode @ 1.0x font scale

## Common Scenarios

### Different States
```kotlin
@ScreenshotTest
@Preview(name = "Empty")
@Composable
fun EmptyPreview() { MyComponent(empty = true) }

@ScreenshotTest
@Preview(name = "Loading")
@Composable
fun LoadingPreview() { MyComponent(loading = true) }

@ScreenshotTest
@Preview(name = "Success")
@Composable
fun SuccessPreview() { MyComponent(data = mockData) }
```

### Animations/Dynamic Content
```kotlin
@ScreenshotTest(threshold = 0.01)  // Allow 1% difference
@Preview
@Composable
fun AnimatedPreview() { AnimatedComponent() }
```

### Landscape
```kotlin
@ScreenshotTest
@Preview(device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun LandscapePreview() { MyComponent() }
```

## File Structure

```
yourmodule/
├── build.gradle.kts                    # Add Roborazzi plugin + dependency
├── screenshots/                        # Generated screenshots (Git LFS)
│   ├── MyComponent_light_normal.png
│   ├── MyComponent_light_xlarge.png
│   └── MyComponent_dark_normal.png
└── src/
    ├── androidMain/kotlin/...          # Your components
    │   └── MyComponent.kt              # Add @ScreenshotTest + @Preview
    └── androidUnitTest/kotlin/...      # Test class
        └── MyModuleSnapshotTest.kt     # Extend BaseSnapshotTest
```

## Advanced Customization

Override properties in your test class:

```kotlin
class CustomSnapshotTest : BaseSnapshotTest() {
    override val packageToScan = "com.example.mymodule"
    
    // Optional overrides
    override val screenshotsDir = "screenshots"
    override val lightModeFontScales = listOf(1.0f, 1.5f, 2.0f)
    override val darkModeFontScales = listOf(1.0f)
    override val testDarkMode = false  // Disable dark mode
    override val includePrivatePreviews = true
    
    @Test
    fun generateSnapshots() {
        generateSnapshots()
    }
}
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| No screenshots generated | Verify `@ScreenshotTest` annotation is on preview |
| Wrong directory (nested) | Use `screenshotsDir = "screenshots"` (no module name) |
| Test not found | Check test is in `androidUnitTest` source set |
| Build error: "Unresolved reference" | Sync Gradle, invalidate caches |
| Tests fail randomly | Increase threshold: `@ScreenshotTest(threshold = 0.01)` |

## Git LFS (One-Time Setup)

```bash
# Install
brew install git-lfs

# Initialize
git lfs install

# Pull screenshots
git lfs pull
```

## Full Documentation

- **Core Module:** [core/snapshot-testing/README.md](core/snapshot-testing/README.md)
- **Example:** [taj/screenshots/README.md](taj/screenshots/README.md)
- **Test Example:** [taj/src/androidUnitTest/.../TajSnapshotTest.kt](taj/src/androidUnitTest/kotlin/xyz/ksharma/krail/taj/snapshot/TajSnapshotTest.kt)

