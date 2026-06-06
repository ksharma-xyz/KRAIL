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

### Em-dashes in `@Preview(name = …)` break the test runner

A preview named `@Preview(name = "Expanded — both fields empty")` either hangs the host test
process indefinitely on macOS or throws `IllegalArgumentException` on Linux CI when Roborazzi
tries to write the resulting PNG. Use plain ASCII for `name` values: a hyphen, colon, or comma
instead of `—`. This also matches the project-wide rule against em-dashes in user-facing copy.

### Previews we deliberately do NOT screenshot test

These render infinite animations that never settle under Robolectric's frame clock and must NOT
be re-annotated with `@ScreenshotTest` until the underlying composables learn to render a static
"resting" frame when `LocalInspectionMode == true`:

- `taj/components/LoadingDotsPill.kt` — `PreviewLoadingDotsPill_Visible` (infinite dots animation)
- `feature/trip-planner/ui/components/loading/LoadingEmojiAnim.kt` — `Preview` (rocket emoji animation)
- `feature/trip-planner/ui/components/DepartureBoardStopCard.kt` — `DepartureBoardStopCardLoadingPreview`
  (passes `isLoading = true`, which renders `LoadingDotsPill`)
- `feature/trip-planner/ui/components/TrackedLegView.kt` — `TrackedLegViewPreview`, `TrackedStopRowPreview`
  (`rememberInfiniteTransition` called unconditionally in `TrackedLegView` — pulse animation always active)

If you add a new preview that uses any of `LoadingDotsPill`, `LoadingEmojiAnim`, `AnimatedDots`,
`Animatable.animateTo` in a loop, or `LaunchedEffect { while (true) … }`, do not add `@ScreenshotTest`
to it. Add the function name to this list instead and we will pick them up when the per-capture
timeout work lands (see "Future work" below).

### Future work

- **Per-capture timeout in `BaseSnapshotTest`** — wrap each `captureScreenshot` call in a 30s
  thread-level timeout so a single hang doesn't block the whole run. Failed captures should log
  `[snapshot] TIMEOUT after 30s: <name>` and continue.
- **Skip annotation** — introduce `@SkipScreenshotTest` (or a `disabled = true` parameter on
  `@ScreenshotTest`) and wire it into the scanner via `excludeIfAnnotatedWithAnyOf`. That way
  the previews above can keep their `@ScreenshotTest` annotation for documentation while the
  scanner skips them.
- **`LocalInspectionMode`-aware loading composables** — make `LoadingDotsPill`, `LoadingEmojiAnim`
  etc. check `LocalInspectionMode.current` and render a static frame in inspection mode. That
  removes the need for the skip list entirely.

## Commands

| Action | Command |
|---|---|
| Record baseline | `./gradlew :module:recordRoborazziAndroidHostTest` |
| Verify against baseline (CI mode) | `./gradlew :module:verifyRoborazziAndroidHostTest` |
| Compare and emit diff PNGs | `./gradlew :module:compareRoborazziAndroidHostTest` |
| Just run the test (debug scanner) | `./gradlew :module:testAndroidHostTest --tests "...SnapshotTest"` |
| Watch progress live | `watch -n 2 'find <module>/screenshots -name "*.png" \| wc -l'` |
| See which preview is currently being captured | `./gradlew :module:recordRoborazziAndroidHostTest --info 2>&1 \| grep Capturing` |

PNG output goes to `<module>/screenshots/`. Diff images from a failed verify go to
`<module>/build/outputs/roborazzi/`. The Roborazzi HTML report is in `<module>/build/reports/roborazzi/`.

## Debugging "Found 0 previews"

The test prints `Found N previews with @ScreenshotTest in <package>` near the start of the run.
If `N == 0`, walk through this checklist before anything else:

1. **Confirm the source set is actually compiled.** Look for "The Kotlin source set androidUnitTest
   was configured but not added to any Kotlin compilation" in gradle output — that means tests live
   in the wrong directory. Move them to `src/androidHostTest/kotlin/` and wire it up in
   `build.gradle.kts` via `getByName("androidHostTest") { kotlin.srcDir(…) }`.
2. **Confirm the scanner matches the annotation.** `BaseSnapshotTest` uses
   `AndroidComposablePreviewScanner`, which scans for `androidx.compose.ui.tooling.preview.Preview`.
   `javap -verbose <YourFile>Kt.class | grep Preview` should show
   `Landroidx/compose/ui/tooling/preview/Preview;` in the constant pool.
3. **Confirm `@ScreenshotTest` is on the same function as a `@Preview`.** It must be on the function
   itself, alongside the `@Preview` (or alongside a custom multi-preview annotation like
   `@PreviewComponent` whose own meta-annotations include `@Preview`).
4. **Run the test class directly** to skip Roborazzi's record machinery while debugging:
   `./gradlew :module:testAndroidHostTest --tests "...SnapshotTest" --info`
5. **Inspect the test XML** at `<module>/build/test-results/testAndroidHostTest/TEST-*SnapshotTest.xml`
   — the `<system-out>` block contains the `Found N previews` line and any per-capture log output.

## Adding a new module

1. Add the dependency in the module's `build.gradle.kts`:
   ```kotlin
   sourceSets {
       commonMain { dependencies { implementation(projects.core.snapshotTestingAnnotations) } }
       getByName("androidHostTest") {
           kotlin.srcDir("src/androidHostTest/kotlin")
           dependencies { implementation(projects.core.snapshotTesting) }
       }
   }
   ```
2. Create `src/androidHostTest/kotlin/.../snapshot/<Module>SnapshotTest.kt` extending `BaseSnapshotTest`.
3. Add `@ScreenshotTest` next to existing `@Preview`/`@PreviewComponent` annotations on the
   composables you want captured (do not add `@Preview` to functions that did not previously have one).
4. Run `./gradlew :module:recordRoborazziAndroidHostTest` to generate the baseline.
5. Verify the new screenshots are LFS-tracked: `git lfs ls-files | grep <module>/screenshots`.
   The root `.gitattributes` already covers `**/screenshots/**/*.png`, so this should be automatic.
6. Commit the test class, `build.gradle.kts` change, and the new PNGs.
