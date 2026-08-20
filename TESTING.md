# Testing KRAIL

The doctrine. Read this before adding tests or onboarding a module; the detail lives in three
sub-pages.

| Page | Answers |
|---|---|
| [`docs/testing/LAYERS.md`](docs/testing/LAYERS.md) | Which kind of test should I write? What does each layer catch, and what can it structurally never catch? |
| [`docs/testing/GUARDS.md`](docs/testing/GUARDS.md) | What is that failure telling me? Every custom detekt rule and guard test, its baseline, and the ratchet contract. |
| [`docs/testing/COVERAGE.md`](docs/testing/COVERAGE.md) | What does the coverage number mean, and what is it not allowed to claim? |

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

**Checklist before merging a test:**

- [ ] Class under test is the **real production class**.
- [ ] Only seam interfaces from the table above are replaced with `:core:testing` fakes.
- [ ] No business logic is re-implemented inside a fake (fakes are dumb: canned data + call records).
- [ ] When testing a `Real*Service`, it's wired to Ktor `MockEngine`, not a hand-rolled `Fake*Service`.
- [ ] Koin is not started; collaborators are passed via constructor.
- [ ] No `object : SomeBoundary { error(...) }` anonymous stub.

### A test must be able to fail

The rule that outranks everything else on this page:

> **A test that cannot fail is worse than no test, because it reads as coverage.**

Before committing a test that pins a fix, revert the fix, watch the test fail, and read the
failure message to confirm it fails *for the reason you think*. When there is no shipped defect
to revert against, prove it another way — instrument the harness and show the thing you assume
is happening is actually happening. The full procedure, with a worked example, is under
"[the discriminating-test rule](docs/testing/LAYERS.md#the-discriminating-test-rule)".

Coverage percentages cannot see this distinction. Nothing in CI can. It is on the author.

---

## The harness — `krailRunTest`

One coroutine/scheduler harness for the whole codebase, in
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

- One shared `TestCoroutineScheduler` for `runTest`, `ioDispatcher` and `mainDispatcher` — no
  more *"Detected use of different schedulers"*.
- `runCurrent()` drains coroutines at the current virtual instant without advancing time;
  `pumpOnce(interval)` is a bounded `advanceTimeBy + runCurrent`.
- **`advanceUntilIdle()` is deliberately not exposed.** Against an infinite poller it spins
  forever — that is how `DepartureBoardRepositoryTest` once produced a 98 GB Gradle log (#1601).

Forbidden when testing infinite pollers: `advanceUntilIdle()`, and forgetting
`cancelAndIgnoreRemainingEvents()` on the Turbine block. `TurbineHygieneTest` enforces the
second; see [GUARDS.md](docs/testing/GUARDS.md).

Full API and the virtual-clock details: [LAYERS.md](docs/testing/LAYERS.md#unit-tests).

---

## Where a new fake goes

| Situation | Location |
|---|---|
| Fake of a cross-cutting boundary interface (`Sandook`, `Analytics`, `*Service`, `Flag`, `RemoteConfig`, …) | `:core:testing/fakes/` — one canonical configurable impl, consumed via `testImplementation(projects.core.testing)`. |
| Reused DTO / response builder | `:core:testing/builders/` |
| Helper used across modules (e.g. analytics-assertion sugar) | `:core:testing/helpers/` |
| Single-feature-only double (an interface defined *inside* one feature module) | Feature-local: `feature/<x>/src/commonTest/.../testfakes/`. Promote to `:core:testing` the moment a 2nd module needs it. |

**Never:**
- An anonymous `object : Boundary { error("x") }` stub.
- A re-declared `private class Fake<Boundary>` in a test file.

`verifyNoAdHocBoundaryFakes` rejects both. Pre-existing offenders sit in
[`config/test-wiring-baseline.txt`](config/test-wiring-baseline.txt), which is **at its enforced
cap** — a new ad-hoc fake cannot be grandfathered by adding a line.

The fakes live in `commonMain`, not `commonTest`, because KMP has no `java-test-fixtures`
equivalent. `verifyTestingModuleUsage` is what stops them shipping in the app.

---

## Test commands

The KMP Android plugin's host test task is `testAndroidHostTest` — **not** `jvmTest`,
`testDebugUnitTest` or `allTests`.

| Scope | Command |
|---|---|
| Single module | `./gradlew :feature:track:ui:testAndroidHostTest` |
| Multiple modules | `./gradlew :a:testAndroidHostTest :b:testAndroidHostTest --continue` |
| All modules | `./gradlew testAndroidHostTest --continue` |
| Static analysis | `./gradlew detekt --continue` |
| The custom detekt rules' own suite | `./gradlew -p gradle/build-logic :detekt-rules:test` |
| Shared tests on iOS (macOS only) | `./gradlew iosUnitTest` |
| Snapshots | `./gradlew :module:recordRoborazziAndroidHostTest` / `verifyRoborazziAndroidHostTest` |
| Coverage | `./gradlew koverHtmlReport` / `koverVerify` |
| Everything static, both platforms | `./scripts/fullQualityChecks.sh` |

> The bare `test` task is not a way to run anything here. `./gradlew test` resolves to exactly
> one task, `:androidApp:test`, and `:androidApp` has no test source set — so it runs zero tests
> and reports success.

---

## The layers

Six of them. Pick the cheapest layer that can actually fail for the reason you care about.
Full detail in [LAYERS.md](docs/testing/LAYERS.md).

| Layer | Catches | Where |
|---|---|---|
| Unit (`krailRunTest`) | logic, state reduction, mapping, time | every module |
| Compose interaction (Robolectric + `createComposeRule`) | what a screen renders, what a tap does, what survives recreation | `:feature:trip-planner:ui` |
| Flow (`FlowTest`) | real screens + real ViewModels wired together, across recreation | `:feature:trip-planner:ui` |
| Snapshot (Roborazzi) | pixels — contrast, clipping, font scale, dark mode | `:taj`, `:feature:trip-planner:ui` |
| iOS (`iosUnitTest`) | Kotlin/Native behaviour differences | 6 modules |
| E2E (Maestro) | does the app launch, can a rider finish a trip | `.maestro/` |

Everything is **host-side**. There are no instrumented `androidTest` targets, and the only
emulator jobs in CI are the Maestro lanes.

Two limits worth knowing before you plan work around them:

- **The iOS lane is small because of a linker wall, not because of the tests.** `:core:testing`
  pulls in the GitLive Firebase SDK, whose iOS klibs need frameworks only the Xcode/SPM build
  supplies — so every module consuming the shared fakes is excluded. 21 modules sit behind it.
- **Snapshot goldens must be recorded on macOS.** Robolectric's native rendering is not
  byte-identical across operating systems, and nothing in the code enforces this — it is a
  property of where the CI job runs.

---

## Guards

Beyond ordinary tests, the repo carries checks that hold **repo-wide invariants**: seven custom
detekt rules, fifteen guard tests, four Gradle verification tasks and one Python
script. Each exists because the mistake it catches actually shipped, is invisible in review, and
is not catchable by a normal unit test.

Full inventory, with every baseline file and where each check runs:
[GUARDS.md](docs/testing/GUARDS.md).

The contract shared by all of their allowlists:

1. The list only ever **shrinks**. Fixing an offender means deleting its line in the same change.
2. Every entry carries its reason.
3. At zero entries, delete the file — the guard becomes an unconditional gate.
4. **A stale entry is a failure.** An allowlist that excuses something which no longer exists
   has rotted into a list of things that were once true.

Adding a line to make a **new** violation pass is not a use of these files.

### CI

Guards run across four places, which is worth knowing when something fails locally but not in CI
or the reverse:

| Check | Runs in |
|---|---|
| `verifyTestWiring`, `verifyTestingModuleUsage`, `verifyNoAdHocBoundaryFakes`, `verifyIosTestClassification` | `code-quality.yml`, step **Verify test wiring** (before detekt, so a misconfigured module fails fast); also every local `check` |
| The 7 custom detekt rules | `code-quality.yml`, step **Run Detekt** |
| The rules' own unit tests | `code-quality.yml`, step **Run build-logic tests** — `./gradlew detekt` cannot run them, see below |
| The guard tests | `code-quality.yml`, step **Run host tests** |
| `check_layout_invariants.py` | `code-quality.yml`, step **Layout invariants**, and `fullQualityChecks.sh` |
| Snapshot verify | `snapshot-verify.yml`, a separate **macOS** job called from `build.yml` |

`:detekt-rules` lives in the `gradle/build-logic` included build. `./gradlew detekt` resolves its
*jar*, so `test` is never in the task graph, and a root-build `test` invocation cannot reach
across the composite boundary. A rule could quietly stop flagging anything with CI still green —
hence the separate step. `fullQualityChecks.sh` does not run them either.

---

## Coverage

```
./gradlew koverHtmlReport      # build/reports/kover/html/index.html
./gradlew koverXmlReport       # build/reports/kover/report.xml — the Codecov upload
./gradlew koverLog             # one line, straight to the console
./gradlew koverVerify          # the floors
```

One merged report for the repo rather than one per module. Kover's report tasks depend on the
`testAndroidHostTest` tasks they measure, so there is no "stale coverage" state to get caught by.

Three things the number does not mean, spelled out in [COVERAGE.md](docs/testing/COVERAGE.md):

- **It measures execution, not assertion.** A test with no assertions covers every line it runs.
- **On a Compose module it is mostly previews.** A snapshot test renders the composable tree and
  marks it covered without checking anything beyond "did not crash".
- **It says nothing about iOS.** Kover instruments JVM bytecode; Kotlin/Native is unsupported
  upstream. `commonMain` *is* counted — via the Android host run — but `iosMain` is invisible.

---

## What this design is preventing

Concrete bug classes the current setup makes hard or impossible:

- **"Detected use of different schedulers."** Eliminated by `krailRunTest` owning one shared
  `TestCoroutineScheduler`.
- **Infinite-poller virtual-time hangs** (the 98 GB log). `advanceUntilIdle()` is not on the
  surface area; `TurbineHygieneTest` requires the cancel.
- **Silent dead tests.** `verifyTestWiring` fails the build when a module has test sources but no
  `testAndroidHostTest` — exactly the bug that hid seven modules' suites for months.
- **Test code leaking into production.** `verifyTestingModuleUsage` forbids any non-test
  configuration from resolving `:core:testing`.
- **Boundary fake drift** (the `FakeFlag` × 6 problem). Canonical fakes are the source of truth;
  new ad-hoc copies fail `verifyNoAdHocBoundaryFakes` against a capped, shrinking baseline.
- **Snapshot drift shipping silently.** Plain `testAndroidHostTest` *records* rather than
  verifies, so a changed golden passes. The explicit `verifyRoborazzi` job is what makes a pixel
  change a failure. Nineteen `:taj` goldens had already drifted by the time that job existed,
  which is what an unrun check costs.
- **Rotation crashes.** `NavKeySerializationConfigTest` catches an unregistered route at test
  time instead of the first time a user rotates; `ViewModelStateDurabilityTest` makes skipping
  `SavedStateHandle` a decision someone writes down.
- **A screen that renders blank on one platform.** `DualPaneCompositingGuardTest` holds the
  iOS map/gradient compositing rule that no compiler can see.
- **Boilerplate per test.** `krailRunTest { }` replaces ~10 lines of dispatcher ceremony; module
  snapshot adoption is a one-line plugin alias.

---

## Pointers

- Canonical harness: [`core/testing/`](core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/)
- Convention plugins: [`gradle/build-logic/convention/`](gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/)
- Custom detekt rules: [`gradle/build-logic/detekt-rules/`](gradle/build-logic/detekt-rules/)
- Snapshot infra: [`core/snapshot-testing/`](core/snapshot-testing/) and [`core/snapshot-testing-annotations/`](core/snapshot-testing-annotations/)
- E2E flows: [`.maestro/README.md`](.maestro/README.md)
- Integration-testing rationale and the discriminating-test procedure: [`docs/INTEGRATION_TESTING_PLAN.md`](docs/INTEGRATION_TESTING_PLAN.md)
- Per-feature UX invariants worth keeping tests in sync with: e.g. [`feature/trip-planner/ui/SEARCH_STOP_UX.md`](feature/trip-planner/ui/SEARCH_STOP_UX.md)
