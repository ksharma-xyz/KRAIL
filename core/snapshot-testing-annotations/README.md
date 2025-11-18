# Snapshot Testing Annotations

This module contains only the `@ScreenshotTest` annotation used to mark Compose `@Preview` functions
for snapshot testing.

## Purpose

This is a **lightweight** module with zero dependencies, designed to be included in production code
without bloating the app size. It was split from the `snapshot-testing` module to avoid transitively
including heavy test dependencies (Roborazzi, Robolectric) in production builds.

## Module Structure

- **`snapshot-testing-annotations`** (this module) - Contains only the annotation
    - ✅ Lightweight (no dependencies)
    - ✅ Can be used in `commonMain` source sets
    - ✅ No Roborazzi/Robolectric bloat

- **`snapshot-testing`** - Contains test infrastructure (BaseSnapshotTest, helpers, etc.)
    - Uses this module for the annotation
    - Contains all the heavy test dependencies
    - Should only be used in test source sets

## Usage in Other Modules

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Lightweight annotation - safe for production
                implementation(projects.core.snapshotTestingAnnotations)
            }
        }

        androidUnitTest {
            dependencies {
                // Heavy test infrastructure - only in tests
                implementation(projects.core.snapshotTesting)
            }
        }
    }
}
```

## Example

```kotlin
@ScreenshotTest
@Preview
@Composable
fun MyButtonPreview() {
    MyButton(text = "Click Me")
}
```
