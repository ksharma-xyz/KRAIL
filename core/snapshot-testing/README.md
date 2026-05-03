# Snapshot Testing

Roborazzi-based snapshot testing infrastructure for KRAIL's KMP `commonMain` previews.
Modules opt in by extending `BaseSnapshotTest` from an `androidHostTest` source set.

## How it works

1. Mark a `@Composable` `@Preview` (or `@PreviewComponent`) in `commonMain` with `@ScreenshotTest`
   from `core:snapshot-testing-annotations`.
2. In the consuming module's `androidHostTest` source set, add a class extending `BaseSnapshotTest`
   and override `packageToScan`.
3. Run `./gradlew :module:recordRoborazziAndroidHostTest` to generate baselines, or
   `verifyRoborazziAndroidHostTest` to check against the recorded baseline.

`BaseSnapshotTest` uses [`AndroidComposablePreviewScanner`](https://github.com/sergio-sastre/ComposablePreviewScanner)
to discover annotated previews via ClassGraph, then captures each one across light/dark mode and
multiple font scales (`SnapshotDefaults`). PNGs land in `<module>/screenshots/` and are tracked via
Git LFS (configured in root `.gitattributes`).

## Per-module setup

```kotlin
// build.gradle.kts of any module that wants snapshots
kotlin {
    androidLibrary {
        withHostTest { isIncludeAndroidResources = true }
    }
    sourceSets {
        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
            dependencies {
                implementation(projects.core.snapshotTesting)
            }
        }
    }
}
```

```kotlin
// src/androidHostTest/kotlin/.../snapshot/MyModuleSnapshotTest.kt
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class MyModuleSnapshotTest : BaseSnapshotTest() {
    override val packageToScan = "xyz.ksharma.krail.mymodule"

    @Test fun generateScreenshots() = generateSnapshots()
}
```

## Gotchas (learned the hard way)

### KMP `androidLibrary` uses `androidHostTest`, not `androidUnitTest`

The `com.android.kotlin.multiplatform.library` plugin compiles host-side tests under the
`androidHostTest` source set. Files placed in `src/androidUnitTest/` are silently ignored
("The Kotlin source set androidUnitTest was configured but not added to any Kotlin compilation").

Use `getByName("androidHostTest") { kotlin.srcDir("src/androidHostTest/kotlin") }` and put test
files in `src/androidHostTest/kotlin/`.

### Roborazzi task name is `recordRoborazziAndroidHostTest`

The historical `recordRoborazziDebug` / `verifyRoborazziDebug` task names do not register on KMP
`androidLibrary` modules. The correct names are `recordRoborazziAndroidHostTest`,
`verifyRoborazziAndroidHostTest`, `compareRoborazziAndroidHostTest`.

### Use `AndroidComposablePreviewScanner`, not `CommonComposablePreviewScanner`

`CommonComposablePreviewScanner` looks for `org.jetbrains.compose.ui.tooling.preview.Preview`
in the bytecode. Our `commonMain` previews import `androidx.compose.ui.tooling.preview.Preview`
(the Compose Multiplatform 1.10+ recommended annotation), so the common scanner finds zero matches
and silently produces no screenshots.

`AndroidComposablePreviewScanner` looks for `androidx.compose.ui.tooling.preview.Preview` and
works correctly across `commonMain`, `androidMain`, and `desktopMain` packages — exactly as the
[library README](https://github.com/sergio-sastre/ComposablePreviewScanner#compose-multiplatform-support)
recommends. The `:common` and `:jvm` artifacts are deprecated and slated for removal in 0.10.0.

### `@ScreenshotTest` must sit alongside a real `@Preview`

The scanner discovers methods by their `@Preview` annotation in the bytecode and then filters by
`@ScreenshotTest`. A function with only `@ScreenshotTest` (no `@Preview` or multi-preview wrapper
that expands to `@Preview`) won't be found.
