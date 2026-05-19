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

Plus `verifyRoborazziAndroidHostTest` runs alongside `testAndroidHostTest`, so a snapshot
diff fails CI (Roborazzi's default mode silently records goldens if missing — explicit
verify catches drift).

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
- **Snapshot drift shipping silently.** `verifyRoborazziAndroidHostTest` runs explicitly
  in CI so a pixel change without a re-record fails the build.
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
