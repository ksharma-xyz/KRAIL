# Layers — what each kind of test is for

Part of [TESTING.md](../../TESTING.md). Read the doctrine there first; this page is the detail.

Six layers. Each one catches a class of bug the layer below it structurally cannot see, and
costs more to run. Pick the cheapest layer that can actually fail for the reason you care
about.

| Layer | Catches | Cost | Where |
|---|---|---|---|
| [Unit](#unit-tests) | logic, state reduction, mapping, time | milliseconds | every module |
| [Compose interaction](#compose-interaction-tests) | what a screen renders and what a tap does | ~1 s per class | `:feature:trip-planner:ui` |
| [Flow](#flow-tests) | several real screens and ViewModels wired together, across recreation | seconds | `:feature:trip-planner:ui` |
| [Snapshot](#snapshot-tests) | pixels — contrast, clipping, font scale, dark mode | seconds, macOS to record | `:taj`, `:feature:trip-planner:ui` |
| [iOS](#ios-tests) | Kotlin/Native behaviour differences | a macOS runner | 6 modules |
| [E2E](#end-to-end-maestro) | does the app launch, can a rider finish a trip | minutes, real device | `.maestro/` |

Everything here is **host-side**. There are no instrumented `androidTest` targets and no
emulator jobs in CI other than the Maestro lanes.

---

## Unit tests

The default. If a bug can be expressed as "this function returned the wrong thing", it belongs
here and nowhere else.

### `krailRunTest` — the one coroutine harness

Lives in
[`core/testing/.../coroutines/`](../../core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/coroutines/).

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

`krailRunTest` wraps `runTest`, builds a `KrailTestScope`, sets `Dispatchers.setMain`, and
resets it in a `finally` **after** `runTest` returns — resetting inside the body kills
`viewModelScope` dispatches during `runTest`'s final drain with an unrelated
`DispatchException`. There is no JUnit `@Rule`, so it works on every KMP target.

The whole `KrailTestScope` surface:

| Member | What it is |
|---|---|
| `scheduler` | the single `TestCoroutineScheduler` |
| `ioDispatcher` | `StandardTestDispatcher(scheduler, name = "krail-io")` |
| `mainDispatcher` | `StandardTestDispatcher(scheduler, name = "krail-main")` |
| `clock` | a virtual `Clock` driven by the same scheduler |
| `runCurrent()` | drains work at the current virtual instant; does not advance time |
| `pumpOnce(Duration)` / `pumpOnce(Long)` | `advanceTimeBy` + `runCurrent` — bounded |

`VIRTUAL_EPOCH` is `2026-04-09T12:00:00Z` — a Thursday, midday UTC. Picked so a test never
lands on a weekend or a day boundary by accident.

**`advanceUntilIdle()` is deliberately not on the scope.** Against an infinite poller it never
terminates: that is how `DepartureBoardRepositoryTest` once produced a 98 GB Gradle log (#1601).
`pumpOnce` is the only sanctioned way to drive a `channelFlow { while (true) { delay(); fetch() } }`.

Forbidden when testing infinite pollers:

- `advanceUntilIdle()` — will not terminate.
- Forgetting `cancelAndIgnoreRemainingEvents()` on the Turbine block — leaks the flow into the
  next test. `TurbineHygieneTest` enforces this one; see [GUARDS.md](GUARDS.md).

### Fakes at the edges

The fake seam is a fixed list (see TESTING.md). Above it, the class under test is the real
production class. `:core:testing` holds the canonical fakes — 23 of them — plus builders and
assertion helpers.

They live in **`commonMain`, not `commonTest`**, because KMP has no `java-test-fixtures`
equivalent. That would let test doubles ship in the app, so `verifyTestingModuleUsage` forbids
any non-test configuration from resolving `:core:testing`.

`:core:testing` depends only on **interface** modules, never on a `Real*` implementation. That
is what keeps it from dragging half the app into every test classpath — and it is why the
module's own coverage number is meaningless (see [COVERAGE.md](COVERAGE.md)).

---

## Compose interaction tests

Robolectric plus `createComposeRule`. They answer "given this state, what is on screen, and
what does a tap do" — questions a ViewModel test cannot ask and a snapshot cannot answer
either, because a snapshot cannot click.

Today they live only in **`:feature:trip-planner:ui`**, under `src/androidHostTest/`: 9 test
classes, around 56 tests. (`:sandook`, `:core:speech-to-text` and `:core:text-recognition` use
Robolectric without Compose, for SQLDelight migrations and Android service wrappers.)

The boilerplate is identical in every class and is currently copy-pasted rather than shared:

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class MyScreenInteractionTest {
    @get:Rule val composeRule = createComposeRule()
```

Content is always wrapped in `PreviewTheme`. `RobolectricDeviceQualifiers` comes from
Roborazzi, not Robolectric.

Representative cases, each showing a different reason to reach for this layer:

| Test | Why it exists |
|---|---|
| `SearchStopScreenInteractionTest` | pill-row visibility across `SearchStopState` shapes, and Home's protection-from-deletion invariant. Its KDoc names what it does **not** cover — drag-to-reorder, sheet flows — which is the right habit |
| `SavedTripsParkRideRestoreTest` | an open Park & Ride card is still open after rotation, via `StateRestorationTester` |
| `TimeTableStopSheetRestoreTest` | the stop-details sheet survives rotation — a `rememberSaveable` vs `remember` bug, invisible to every static check |
| `AiInputBarLayoutTest` | a layout-invariant probe pinning the fix in [`docs/learning/2026-08-16-clipped-inside-its-own-parent.md`](../learning/2026-08-16-clipped-inside-its-own-parent.md) |
| `AskKrailStatusLineTest` | a precedence contract: resolved beats busy, busy beats hint |

The two restore tests are the cheap host-side answer to "does this screen survive rotation".
They are why the rotation E2E flow does not need to run on every PR.

---

## Flow tests

One level above interaction tests: several **real** screens, **real** ViewModels and the real
navigation entry wired together, driven the way a rider would drive them, across process
recreation.

Harness:
[`feature/trip-planner/ui/src/androidHostTest/.../flow/`](../../feature/trip-planner/ui/src/androidHostTest/kotlin/xyz/ksharma/krail/trip/planner/ui/flow/)

| File | Role |
|---|---|
| `FlowTest.kt` | `abstract class FlowTest` — annotations, compose rule, Koin lifecycle, helpers |
| `HomeFlowFakes.kt` | edge fakes plus the three real ViewModels and the Koin module |
| `RecordingTripPlannerNavigator.kt` | records navigation requests instead of navigating |

### What it actually composes

The **real `SavedTripsEntry`**, through the real `entryProvider` DSL:

```kotlin
val provider = entryProvider<NavKey> { SavedTripsEntry(navigator) }
provider(SavedTripsRoute).Content()
```

`NavDisplay` and its decorators are deliberately cut, so ViewModels scope to the Activity store
rather than the nav entry. The ViewModels are real — `AiSearchInputViewModel` with its real
resolver chain and real `RiderOriginLocator`, `SavedTripsViewModel`, `MapStopSelectionViewModel`.
Only module edges are faked: `FakeSandook`, `FakeAiTextService`, `FakeSpeechToTextService`,
`FakeStopResultsManager`, `FakeNearbyStopsRepository`, `FakeFlag`.

Koin is started in `@Before` with the fakes module. `@After` calls `stopKoin()` **and** clears
`ResultEventBus`'s `StopSelectedResult` — a process singleton that otherwise leaks between
tests.

### Helpers

| Helper | Does |
|---|---|
| `launchHome()` | composes the entry and settles it |
| `leaveAndReturn()` | navigates away and back |
| `recreate()` | drives `StateRestorationTester` — the rotation/process-death path |
| `letTimePass(millis)` | advances **both** clocks |
| `askKrail(sentence)` | drives the Ask KRAIL input end to end |
| `pickStop(StopSelectedResult)` | delivers a stop selection through the result bus |

Two details in there are hard-won and easy to undo:

- **`letTimePass` advances two clocks**, `composeRule.mainClock.advanceTimeBy` *and*
  `shadowOf(Looper.getMainLooper()).idleFor`, because Compose animations and ViewModel `delay()`
  run on different clocks. Advancing one leaves the other where it was.
- **`askKrail` sends its three events one composition apart.** taj's `TextField` echoes its
  contents from a `LaunchedEffect`; a batched send steps the phase back to IDLE mid-resolve and
  the test measures the wrong thing.

### When to write one

A flow test is the right layer when the bug **lives between** components: state that survives
one screen but not the handoff to the next, a result bus delivering to a recreated ViewModel, a
value that is correct until the Activity is rebuilt. If the bug fits inside one screen, use an
interaction test — it is faster and its failure message points at one place.

There are **two** flow tests today. That is deliberate: they are the most expensive tests here
to write and the easiest to write badly.

### The discriminating-test rule

From [`docs/INTEGRATION_TESTING_PLAN.md`](../INTEGRATION_TESTING_PLAN.md), "Prove the test
discriminates" — the single most important rule on this page:

> A flow test that cannot fail is worse than no test, because it reads as coverage.

Before committing one: revert the fix it pins (`git revert --no-commit <fix>`), **watch it
fail**, read the failure message to confirm it fails *for the reason you think*, then restore.
For the Ask KRAIL replay test the pre-fix run fails with the AI's origin back on the row:

```
Failed: assertDoesNotExist.
Reason: Did not expect any node but found '1' node that satisfies:
  (Text + InputText + EditableText contains 'Central Station')
```

When there is no shipped defect to revert against, prove it another way. `SearchStopRoundTripFlowTest`
used a `DisposableEffect` probe in the harness entry to show that `recreate()` really disposes
and recomposes, rather than quietly doing nothing.

This rule is not flow-test-specific in principle. It is stated here because this is the layer
where a vacuously-passing test is most likely and least visible.

---

## Snapshot tests

Roborazzi. Infrastructure in [`core/snapshot-testing/`](../../core/snapshot-testing/),
annotations in `core/snapshot-testing-annotations/`.

Any `@PreviewComponent` / `@PreviewScreen` function also annotated `@ScreenshotTest` gets shot
by the next record run. Two modules own goldens: **`:taj`** (114 PNGs) and
**`:feature:trip-planner:ui`** (279 PNGs), committed under `<module>/screenshots/` and tracked
by Git LFS.

### Tasks

| Action | Task |
|---|---|
| Record | `./gradlew :module:recordRoborazziAndroidHostTest` |
| Verify | `./gradlew :module:verifyRoborazziAndroidHostTest` |
| Compare (writes diff PNGs) | `./gradlew :module:compareRoborazziAndroidHostTest` |

The `*Debug`-suffixed task names do **not** exist on KMP `androidLibrary` modules.

Plain `testAndroidHostTest` captures in Roborazzi's default mode, which silently records a
missing or changed golden and still passes. **Explicit verify is what turns a pixel change into
a failure** — which is the entire reason the CI job below exists.

Each preview yields three images: light at 1× and 2× font scale, dark at 1×. Previews are
deduped by `declaringClass#methodName`, because the project preview annotations expand to
several `@Preview` variants that render identically under this harness.

### Onboarding a module

```kotlin
plugins { alias(libs.plugins.krail.snapshot.testing) }   // roborazzi + both snapshot deps

androidLibrary {
    withHostTest { isIncludeAndroidResources = true }    // Roborazzi needs Android resources
    androidResources { enable = true }                   // MANDATORY for AGP 9
}
```

Plus a ~10-line test class extending `BaseSnapshotTest` with `packageToScan` set, then
`recordRoborazziAndroidHostTest`.

### macOS only, and why

`snapshot-verify.yml` runs on `macos-latest`, called from `build.yml` in parallel with
`code-quality`. The goldens are recorded on a Mac and Robolectric's native rendering is **not
byte-identical across operating systems** — font rasterisation and the bundled Skia both differ.
On `ubuntu-latest` it fails on files that are correct.

The rule is enforced by **job placement, not by code**: nothing inside `BaseSnapshotTest`
checks the host. Recording on Linux produces goldens that fail everywhere.

Three other things about that job are load-bearing:

- **`lfs: true` on the checkout.** Without it the runner has 130-byte pointer files and every
  comparison fails on an undecodable PNG.
- **One step per golden-owning module, named explicitly.** Onboarding a third module means
  adding a step — the cost of a macOS runner should not grow by accident.
- **Both steps block.** The theme-transition race that kept trip-planner on `continue-on-error`
  is fixed.

### The theme-transition race, and how it was fixed

`KrailTheme` animates `colors.surface` against a multi-stage target
(`taj/animations/ThemeTransitionAnimations.kt`). The harness used to shoot a preview as soon as
the view attached, mid-transition, so a golden recorded a background one shade off — and *which*
files that hit changed from run to run.

`settleAnimations()` now runs before every capture: pause the Robolectric choreographer, then
idle the main looper for a fixed 2 s, longer than the transition's worst case. Two details are
the whole fix:

- **The advance is a fixed duration, not a wait for quiescence.** Several previews animate
  forever, so an idle-wait hangs on them.
- **The choreographer must be paused first.** Left running, Robolectric answers
  `nativeScheduleVsync` immediately without moving the clock, so an endless animation re-arms a
  frame inside the same idle window and `idleFor` never returns — one preview sat at 100% CPU
  through 170 000+ frames. Paused, each vsync lands at the next frame boundary, the window is
  capped at 125 frames, and an endless animation lands on a fixed frame.

Re-recording under the settled clock rewrote every golden in both modules: the committed images
had been captured during the transition's 80 ms glow stage rather than on the settled surface.
Two consecutive record runs then produced byte-identical sets.

### Two ways to skip a preview — do not confuse them

| Mechanism | Effect | Used by |
|---|---|---|
| `excludedPreviewNames` in the module's test class | the preview is enumerated but not shot | `:feature:trip-planner:ui`, for `PreviewMapStopSelectionPane_Loading` |
| Simply not annotating it `@ScreenshotTest` | the preview is never a snapshot subject at all | `:taj`, for `PreviewLoadingDotsPill_Visible` and two others |

`:taj` overrides `excludedPreviewNames` with **nothing** — its infinite-animation previews carry
a bare `@Preview` and a comment saying why. Both mechanisms are legitimate; pick the second when
the preview should never be a snapshot, and the first when it is one you are temporarily
skipping.

`PreviewMapStopSelectionPane_Loading` renders an indeterminate `CircularProgressIndicator`, so
its captured frame is decided by the animation clock no matter how well the theme settles.

---

## iOS tests

`commonTest` has always *compiled* for iOS. Until the `iosUnitTest` lane it never **ran** there,
so every Kotlin/Native behaviour difference — freezing, date and number formatting, `kotlin.time`,
`Char` handling — was invisible.

```
./gradlew iosUnitTest          # macOS + Xcode only
```

Registered on the root project by `configureIosUnitTestLane()` in
[`IosUnitTests.kt`](../../gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/IosUnitTests.kt).
It first runs `verifyIosTestClassification`, then `iosSimulatorArm64Test` for every module in
the lane. Off macOS it fails in `doFirst` with an explicit message rather than pretending to
pass.

CI: [`.github/workflows/ios-unit-tests.yml`](../../.github/workflows/ios-unit-tests.yml) on
`macos-latest`, for pushes to `main` and PRs touching shared sources or build config.

### What runs on iOS

Six modules: `:core:date-time`, `:core:deeplink`, `:core:navigation`, `:core:transport`,
`:feature:debug-settings:store`, `:taj`.

### The Firebase linking wall

**21 modules are excluded, and all but one for the same single reason.** `:core:analytics` and
`:core:remote-config` depend on the GitLive Firebase Kotlin SDK, whose iOS klibs declare
`-framework FirebaseCore`. Those frameworks come from the Xcode project's SPM integration, so
when Gradle links a standalone `.kexe` test binary the linker cannot find them:

```
ld: framework 'FirebaseCore' not found
> Task :core:analytics:linkDebugTestIosSimulatorArm64 FAILED
```

`:core:testing` depends on both, so **every module that consumes the shared fakes inherits the
wall.** That is what keeps the lane small — not anything wrong with the tests, and not a
judgement about those modules. The include list is exactly the set of modules that do not touch
the shared fakes.

Widening it means either giving Gradle its own copy of the Firebase iOS frameworks (the repo
already drives SPM from Gradle for MapLibre) or splitting the Firebase-backed implementations
out from the interfaces the fakes need. Both are real work; neither is a test-code change.

A second, cheaper blocker affects two modules on top of the wall: **backtick test names
containing `,`**, which Kotlin/Native rejects where the JVM accepts them
(`Name contains illegal characters: ","`). `:feature:track:network` and
`:feature:trip-planner:ui` carry such names. Renaming is trivial but pointless on its own,
since both also sit behind the wall. `:taj` joined the lane by fixing exactly this.

Entries leave the exclusion map **by being fixed and moved to the include list, never by being
deleted.** `verifyIosTestClassification` enforces both directions.

Robolectric, Roborazzi and Compose UI tests stay host-only by construction: they live in
`androidHostTest`, which the iOS compilation never sees.

### Shared `runComposeUiTest` — parked, not rejected

Spiked in `:taj` with `compose.uiTest` driving a real `Button` through `setContent` /
`onNodeWithText` / `performClick`:

- On **iOS it works** — green under `iosSimulatorArm64Test`, no extra setup.
- In **`commonTest` it cannot stay**: the same source also expands into `androidHostTest`, and
  Android's `runComposeUiTest` needs a Robolectric runner and `@Config`, which `commonTest` has
  no way to express. The Android run dies with `NullPointerException: … "android.os.Build.FINGERPRINT" is null`.
- Moving it to **`iosTest` works on both** (iOS runs it, the host ignores it) — but then it is an
  iOS-only test, not a shared one.

**Parked.** An `iosTest`-only Compose test duplicates coverage `:taj` already has via Robolectric
and Roborazzi, and the modules where a shared UI test would genuinely pay off
(`:feature:trip-planner:ui`, `:feature:departures:ui`) are behind the Firebase wall anyway. Worth
revisiting the day that wall comes down — the API is not the obstacle.

---

## End-to-end (Maestro)

Host tests prove logic; snapshots prove pixels. Neither can say whether the app launches, whether
a rider can reach a timetable, or whether a screen survives having its Activity destroyed under
it. [Maestro](https://maestro.mobile.dev) flows in [`.maestro/`](../../.maestro/) drive the real
app on a real device to cover exactly that gap. Full detail in
[`.maestro/README.md`](../../.maestro/README.md).

Verified against **Maestro 2.8.0**; `setOrientation` needs 2.x.

### Lanes

| Lane | Runs | Workflow |
|---|---|---|
| `.maestro/smoke/` | every non-draft PR to `main` / `prod/*` | `maestro-pr-smoke.yml` |
| `.maestro/nightly/` | 03:00 AEST cron, `prod/**`, manual dispatch | `maestro-nightly.yml` |

`.maestro/shared/` holds helper flows called via `runFlow`. It sits outside both lanes so a
directory run never treats one as a test.

Smoke gates a merge and must stay fast: two flows, cold launch and planning a trip against the
real API. Nightly runs the **whole** tree on Android and, in a separate `macos-15` job, on an
iPhone simulator — and gates nothing. Neither nightly job merges, tags or publishes anything; a
red nightly is a signal for a human.

**The rotation sweep is a nightly flow, not a smoke one.** Rotating a software-rendered CI
emulator was the dominant flake source in a lane that must never retry. What it covers is
already covered per-PR host-side, where recreation is deterministic: two `StateRestorationTester`
tests drive the same screens, and `NavKeySerializationConfigTest` catches the unregistered-route
crash rotation would otherwise be first to find. The sweep still runs nightly, where it exercises
the real window-level path those cannot.

### The `APP_ID` convention

One set of flows serves both platforms, so the app id is a parameter, not a literal. It **must**
be passed with `-e`: a plain shell variable never reaches the flow, and an in-file `env:` default
would silently take precedence over `-e` and make the parameter impossible to override.

| Target | `APP_ID` |
|---|---|
| Android debug | `xyz.ksharma.krail.debug` (note the suffix) |
| Android release | `xyz.ksharma.krail` |
| iOS | `xyz.ksharma.krail` |

```sh
./gradlew :androidApp:installDebug
maestro test -e APP_ID=xyz.ksharma.krail.debug .maestro/smoke/
```

### `ci/run-flows.sh`

```
Usage: run-flows.sh <app-id> <flows-path> <logcat-file> [device-serial]
```

Four positional arguments, no flags. It runs the lane, then — **while the emulator is still
alive** — dumps logcat, copies `~/.maestro/tests` into the workspace, and fails the step if
`FATAL EXCEPTION` appears in the log, separately from the flow result (a crash in a background
coroutine does not necessarily fail a flow).

It is a file rather than inline YAML for one specific reason:
`reactivecircus/android-emulator-runner` executes its `script:` **one line at a time**, each in
its own `sh -c`. Shell state does not carry across those invocations, so `set +e` on one line
cannot protect the next, and the step aborts the instant any line exits non-zero — which is how
the first two runs of this lane finished with no artifact at all. One line in the workflow, one
shell in the script, all the control flow intact.

Pass a device serial when reproducing locally: with more than one device attached Maestro shards
the flows across all of them, which fails in ways that look like app bugs and are not.

### Selectors

Flows select on `testTag` ids, never on visible copy, so a wording change cannot break a flow and
a failure always means behaviour changed. Tags are declared in `TripPlannerTestTags` and
`DebugSettingsTestTags` and are **public API** to `.maestro/` — grep there before renaming one.

Android surfaces them as accessibility `resource-id`s via `exposeTestTagsToUiAutomation()` at the
app root; on iOS, Compose Multiplatform publishes them as `accessibilityIdentifier` with no
opt-in. The same `id:` selector works on both.

### Flake policy

**The PR lane never retries.** A flake that can block a merge gets fixed, not re-run. The flows
are built for that: they wait rather than assert after anything asynchronous, and scroll to list
items instead of asserting them in place, because Maestro matches only what is on screen.

**Both nightly legs retry once.** That lane reports rather than gates, so one retry buys signal
without hiding a real break — a genuine regression fails twice. Android passes
`MAESTRO_RETRIES=1` to `run-flows.sh`; the iOS leg wraps its suite in bash, and is the flakier
of the two anyway (boot races, window-server hiccups). Each retry force-stops the app first, so
the second attempt starts from a launch rather than from wherever the failed flow left off.
