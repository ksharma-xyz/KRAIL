# Testing KRAIL

A short guide. Read this before adding tests or onboarding a new module.

---

## Doctrine — what you fake and what stays real

The **fake seam** is exactly these layers. Everything **above** them in a test is the real
production class.

| FAKE (the edges) | REAL (above the seam) |
|---|---|
| `Sandook`, `SandookPreferences` (SQLDelight) | `*Repository`, cache layers, mappers |
| `Flag` / `RemoteConfig` | feature gating, ordering engines |
| `Analytics` | anything emitting analytics |
| `TripPlanningService`, `DeparturesService`, `*Service` interfaces | repositories, managers, fuzzy/ranking, Molecule ViewModels |
| Ktor `HttpClient` via MockEngine — when testing a `Real*Service` itself | the `Real*Service`'s JSON parsing / error mapping — tested for real |
| `UserLocationManager`, `PendingDeepLinkManager`, `AppVersionManager`, `AppInfoProvider`, `ShareManager`, platform `expect/actual` | navigation, state reducers |
| `Dispatchers` (inject `ioDispatcher` / `mainDispatcher`), `Clock` | all time-dependent logic |

> Rule of thumb: if you'd hit network, disk, the system clock, or a platform service in
> production, that's a fake. Everything else is the real class.

**PR checklist before merging a test:**

- [ ] Class under test is the **real production class**.
- [ ] Only seam interfaces from the table above are replaced with `:core:testing` fakes.
- [ ] No business logic is re-implemented inside a fake (fakes are dumb: canned data + call records).
- [ ] When testing a `Real*Service`, it's wired to Ktor `MockEngine`, not a hand-rolled `Fake*Service`.
- [ ] Koin is not started; collaborators are passed via constructor.
- [ ] No `object : SomeBoundary { error(...) }` anonymous stub.

---

## The harness — `krailRunTest`

Single coroutine/scheduler harness for the whole codebase. Lives in
[`:core:testing`](core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/coroutines/).

```kotlin
@Test fun `loads data`() = krailRunTest {
    val repo = MyRepository(
        service = FakeMyService(),
        ioDispatcher = ioDispatcher,        // same scheduler as runTest
    )
    repo.observe().test {                   // Turbine
        runCurrent()
        assertEquals(Initial, awaitItem())
        pumpOnce(refreshIntervalMs)         // ONE poll cycle for an infinite-poller flow
        assertEquals(Refreshed, awaitItem())
        cancelAndIgnoreRemainingEvents()    // never let an infinite flow spin
    }
}
```

What you get:

- One shared `TestCoroutineScheduler` for `runTest`, `ioDispatcher`, `mainDispatcher` — no
  more *"Detected use of different schedulers"*.
- `runCurrent()` drains coroutines at the current virtual instant; doesn't advance time.
- `pumpOnce(interval)` = bounded `advanceTimeBy + runCurrent`. The **only** safe way to
  drive a `channelFlow { while (true) { delay(); fetch() } }` poller.
- `advanceUntilIdle()` is **deliberately not exposed** on `KrailTestScope`. Calling it
  against an infinite poller spins forever — that's how `DepartureBoardRepositoryTest`
  previously produced a 98 GB Gradle log (#1601).

**Forbidden** when testing infinite pollers:
- `advanceUntilIdle()` (won't terminate).
- Forgetting `cancelAndIgnoreRemainingEvents()` on the Turbine block (leaks the flow into
  the next test).

---

## Where a new fake goes

| Situation | Location |
|---|---|
| Fake of a cross-cutting boundary interface (`Sandook`, `Analytics`, `*Service`, `Flag`, `RemoteConfig`, …) | `:core:testing/fakes/` — one canonical configurable impl, consumed via `testImplementation(projects.core.testing)`. |
| Reused DTO / response builder | `:core:testing/builders/` |
| Helper used across modules (e.g. analytics-assertion sugar) | `:core:testing/helpers/` |
| Single-feature-only double (an interface defined *inside* one feature module) | Feature-local: `feature/<x>/src/commonTest/.../testfakes/` (e.g. `FakeStopResultsManager` in `trip-planner/ui`). Promote to `:core:testing` the moment a 2nd module needs it. |

**Never:**
- An anonymous `object : Boundary { error("x") }` stub. The CI guardrail
  (`verifyNoAdHocBoundaryFakes`) rejects new ones.
- A re-declared `private class Fake<Boundary>` in a test file. Same rule.

The few pre-existing offenders are listed in [`config/test-wiring-baseline.txt`](config/test-wiring-baseline.txt).
The baseline file shrinks every time a migration replaces one with the canonical fake; it
never grows.

---

## CI guardrails

Three verification tasks run on every PR via `.github/workflows/code-quality.yml` and are
also wired into `check`:

| Task | Fails when |
|---|---|
| `verifyTestWiring` | A module has Kotlin files under `src/commonTest`/`src/androidHostTest`/`src/androidUnitTest` but no `testAndroidHostTest` task — i.e. someone forgot `withHostTest {}` so CI was silently skipping the suite. |
| `verifyTestingModuleUsage` | Any `commonMain` / `androidMain` / `iosMain` configuration resolves `:core:testing`. Stops fakes from shipping in the app and breaks any `:core:testing → feature → :core:testing` cycle. |
| `verifyNoAdHocBoundaryFakes` | New `object : Boundary { … }` stub or `private class Fake<Boundary>` appears in any test source set. Existing offenders are grandfathered via the baseline file above. |

The same workflow also runs the custom detekt rules' own suite:

```
./gradlew -p gradle/build-logic :detekt-rules:test
```

`:detekt-rules` lives in the `gradle/build-logic` included build. `./gradlew detekt` compiles
it but never runs its tests, and a root-build `test` invocation cannot reach across the
composite boundary — so `PublicImplementationClassTest` had never executed in CI. A rule could
have quietly stopped flagging anything with CI still green. `-p` runs the task inside the
included build directly.

Note on the bare `test` task: it is not a way to run anything here. `./gradlew test` resolves
to exactly one task, `:androidApp:test`, and `:androidApp` has no test source set, so it runs
zero tests. `testAndroidHostTest` is the only host-test task name that matches KMP modules.

Snapshot verification is a separate job, `.github/workflows/snapshot-verify.yml`, called
from `build.yml` in parallel with `code-quality`. It runs one step per golden-owning module:

| Step | Task | Blocking |
|---|---|---|
| `verify-taj` | `:taj:verifyRoborazziAndroidHostTest` | yes |
| `verify-trip-planner` | `:feature:trip-planner:ui:verifyRoborazziAndroidHostTest` | yes |

Four things about that job are load-bearing:

- **It is a separate job, not a step in `code-quality`.** It runs on `macos-latest`, because
  the goldens are recorded on a Mac and Robolectric's native rendering is not byte-identical
  across operating systems. On `ubuntu-latest` it fails on files that are correct.
- **The checkout sets `lfs: true`.** Goldens are Git LFS objects; without it the runner has
  pointer files on disk and every comparison fails on an undecodable PNG.
- **The modules are named explicitly**, one step each. Onboarding a third module means adding
  a step — the cost of a macOS runner should not grow by accident.
- **Both steps block.** The theme-transition race that kept trip-planner on
  `continue-on-error` is fixed. See below.

The reason the job has to exist at all: `testAndroidHostTest` on its own captures in
Roborazzi's default mode, which silently records a missing or changed golden and still
passes. Explicit verify is what turns a pixel change into a failure. When either step
reports a diff the job uploads the reference/diff/new composites from
`**/build/outputs/roborazzi/**` as an artifact.

### The theme-transition race, and how it was fixed

`KrailTheme` animates `colors.surface` with `animateColorAsState` against a multi-stage target
(`taj/animations/ThemeTransitionAnimations.kt`). The harness used to shoot a preview as soon as
the view was attached, mid-transition, so a golden recorded a background one shade off and
which files that hit changed from run to run — which is why trip-planner ran with
`continue-on-error` for as long as it did.

`BaseSnapshotTest.captureScreenshot` now settles the animation clock before every capture:
pause the Robolectric choreographer, then idle the main looper for a fixed 2 s, which is longer
than the transition's worst case (80 ms + 100 ms of staged delay plus a 1500 ms surface tween).
Two details are the whole fix:

- **The advance is a fixed duration, not a wait for quiescence.** Several previews animate
  forever, so an idle-wait would hang on them.
- **The choreographer has to be paused first.** Left running, Robolectric answers
  `nativeScheduleVsync` immediately without moving the clock, so an endless animation re-arms a
  frame inside the same idle window and `idleFor` never returns — one preview sat at 100% CPU
  through 170 000+ frames. Paused, each vsync is posted at the next frame boundary, so the
  window is capped at 125 frames and an endless animation lands on a fixed frame.

Re-recording under the settled clock rewrote every golden in both modules: the committed
images had been captured during the transition's 80 ms glow stage (`#E8ECF0`) rather than on
the settled surface (`#FFFFFF`). Two consecutive record runs then produced byte-identical sets,
and the step blocks.

One preview is excluded outright rather than relying on that settle:
`PreviewMapStopSelectionPane_Loading` renders an indeterminate `CircularProgressIndicator`, so
its captured frame is decided by the animation clock no matter how well the theme settles. It
is listed in `TripPlannerUiSnapshotTest.excludedPreviewNames`, the same treatment taj gives
`PreviewLoadingDotsPill_Visible`.

---

## Test commands

The KMP Android plugin's host test task is `testAndroidHostTest` — **not** `jvmTest` or
`testDebugUnitTest`.

| Scope | Command |
|---|---|
| Single module | `./gradlew :feature:track:ui:testAndroidHostTest` |
| Multiple modules | `./gradlew :a:testAndroidHostTest :b:testAndroidHostTest --continue` |
| All modules | `./gradlew testAndroidHostTest --continue` |

`./gradlew detekt --continue` rounds out the pre-push gate. Both must be green locally
before pushing.

---

## Running the shared tests on iOS

`commonTest` has always *compiled* for iOS. Until the `iosUnitTest` lane it never **ran**
there, so every Kotlin/Native behaviour difference — freezing, date and number formatting,
`kotlin.time`, `Char` handling — was invisible.

```
./gradlew iosUnitTest          # macOS + Xcode only
```

That aggregate runs `iosSimulatorArm64Test` for each module in the lane and first runs
`verifyIosTestClassification`, which fails if a module has shared test sources but is in
neither the include nor the exclude list. Both lists, and the reason for every exclusion,
live in one place:
[`gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/IosUnitTests.kt`](gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/IosUnitTests.kt).
`verifyIosTestClassification` also runs on the cheap Ubuntu runner in `code-quality.yml`, so
drift is caught on every PR rather than only on the macOS lane.

CI: [`.github/workflows/ios-unit-tests.yml`](.github/workflows/ios-unit-tests.yml) on
`macos-latest`, for pushes to `main` and PRs touching shared sources or build config.

### What runs on iOS

| Module | Tests |
|---|---|
| `:core:date-time` | 27 |
| `:core:deeplink` | 18 |
| `:core:transport` | 15 |
| `:taj` | 8 |
| `:feature:debug-settings:store` | 6 |

### What is host-only, and why

**Firebase iOS frameworks — 19 of the 24 modules with shared tests.** `:core:analytics` and
`:core:remote-config` depend on the GitLive Firebase Kotlin SDK, whose iOS klibs declare
`-framework FirebaseCore`. Those frameworks are supplied by the Xcode project's SPM
integration, so when Gradle links a standalone `.kexe` test binary the linker cannot find
them:

```
ld: framework 'FirebaseCore' not found
> Task :core:analytics:linkDebugTestIosSimulatorArm64 FAILED
```

`:core:testing` depends on both, so **every module that consumes the shared fakes inherits
the wall** — that is what keeps the lane small, not anything wrong with the tests. Widening
it means giving Gradle its own copy of the Firebase iOS frameworks (the repo already drives
SPM from Gradle for MapLibre, via `krail.maplibre`), or splitting the Firebase-backed
implementations out from the interfaces the fakes need. Both are real work, neither is a
test-code change.

**Backtick test names containing `,`.** Kotlin/Native rejects them where the JVM accepts
them:

```
e: ThemeContrastTest.kt:35:9 Name contains illegal characters: ",".
```

Cheap to fix — rename the test. `:taj` joined the lane this way. `:feature:track:network`
and `:feature:trip-planner:ui` still carry such names, but both also sit behind the Firebase
wall, so renaming alone would not get them running.

**Robolectric, Roborazzi and Compose UI tests** stay host-only by construction — they live in
`androidHostTest`, which the iOS compilation never sees.

### Shared Compose UI tests (`runComposeUiTest`) — parked, not rejected

Spiked in `:taj` with `compose.uiTest` and an `@OptIn(ExperimentalTestApi)` test driving a real
`Button` through `setContent` / `onNodeWithText` / `performClick`. Findings:

- On **iOS it works**. The test ran green under `iosSimulatorArm64Test` with no extra setup.
- In **`commonTest` it cannot stay**, because the same source also expands into
  `androidHostTest`, and Android's `runComposeUiTest` needs a Robolectric runner and
  `@Config`, which `commonTest` has no way to express. The Android run dies with
  `NullPointerException: … "android.os.Build.FINGERPRINT" is null` from
  `RobolectricIdlingStrategy`.
- Moving it to **`iosTest` works on both** (iOS runs it, the host ignores it) — but then it is
  an iOS-only test, not a shared one.

**Parked.** An `iosTest`-only Compose test duplicates coverage `:taj` already has via
Robolectric and Roborazzi, and the modules where a shared UI test would genuinely pay off
(`:feature:trip-planner:ui`, `:feature:departures:ui`) are all behind the Firebase link wall,
so they could not run it anyway. Worth revisiting the day that wall comes down — the API
itself is not the obstacle.

---

## Snapshot testing

The annotation-driven generation flow is preserved: any `@PreviewComponent` /
`@PreviewScreen` annotated `@ScreenshotTest` gets shot by the next
`recordRoborazziAndroidHostTest` run.

Onboarding a new UI module is one line plus a small test class:

```kotlin
// In module's build.gradle.kts
plugins {
    alias(libs.plugins.krail.snapshot.testing)   // adds roborazzi + both core/snapshot* deps
    // …other plugins
}

androidLibrary {
    withHostTest {
        isIncludeAndroidResources = true         // Roborazzi needs Android resources
    }
    androidResources {
        enable = true                            // MANDATORY for AGP 9
    }
}
```

Then a 10-line `<Module>SnapshotTest.kt` extending [`BaseSnapshotTest`](core/snapshot-testing/src/androidMain/kotlin/xyz/ksharma/krail/core/snapshot/BaseSnapshotTest.kt):

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class MyUiSnapshotTest : BaseSnapshotTest() {
    override val packageToScan = "xyz.ksharma.krail.my.ui"

    // Skip previews Robolectric hangs on (infinite shimmer, indeterminate loading).
    override val excludedPreviewNames = setOf("PreviewLoadingDotsPill_Visible")

    @Test fun `generate snapshots`() = generateSnapshots()
}
```

Then `./gradlew :my:module:recordRoborazziAndroidHostTest` captures the goldens (PNGs go to
`<module>/screenshots/`, tracked by Git LFS).

---

## What this design is preventing

Concrete bug classes the current setup makes hard or impossible:

- **"Detected use of different schedulers."** Eliminated by `krailRunTest` owning one
  shared `TestCoroutineScheduler`.
- **Infinite-poller virtual-time hangs** (the 98 GB log). `advanceUntilIdle()` isn't on
  the surface area; only bounded `runCurrent` / `pumpOnce`. Mandatory
  `cancelAndIgnoreRemainingEvents()` in every Turbine block.
- **Silent dead tests.** `verifyTestWiring` fails the build when a module has test sources
  but no `testAndroidHostTest` — exactly the bug that hid 7 modules' suites for months.
- **Test code leaking into production.** `verifyTestingModuleUsage` forbids any non-test
  configuration from resolving `:core:testing`.
- **Boundary fake drift** (the `FakeFlag` × 6 problem). The canonical fakes are the
  shared source of truth; new ad-hoc copies fail
  `verifyNoAdHocBoundaryFakes`; pre-existing ones are in a shrinking baseline.
- **Snapshot drift shipping silently.** The `snapshot-verify` job runs
  `verifyRoborazziAndroidHostTest` explicitly, on macOS, with LFS goldens checked out, so a
  pixel change without a re-record fails the build for both golden-owning modules. Nineteen
  `:taj` goldens had already drifted by the time that job existed, which is what an unrun
  check costs.
- **Boilerplate per test.** `krailRunTest { }` replaces ~10 lines of dispatcher
  setup/teardown ceremony. New module snapshot adoption is one-line plugin alias instead
  of three duplicated build-config blocks.

---

## Pointers

- Plan that drove this work: [`.claude/plans/on-a-worktee-look-expressive-cat.md`](.claude/plans/on-a-worktee-look-expressive-cat.md) (in the parent checkout).
- Canonical harness: [`core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/`](core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/)
- Convention plugin source: [`gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/`](gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/)
- Snapshot infra: [`core/snapshot-testing/`](core/snapshot-testing/) and [`core/snapshot-testing-annotations/`](core/snapshot-testing-annotations/)
- Per-feature UX invariants worth keeping tests in sync with: e.g. [`feature/trip-planner/ui/SEARCH_STOP_UX.md`](feature/trip-planner/ui/SEARCH_STOP_UX.md)
