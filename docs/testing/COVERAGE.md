# Coverage — what the number means

Part of [TESTING.md](../../TESTING.md). Read this before quoting a coverage percentage at
anyone, and before changing a Kover filter.

A coverage percentage is a **measurement of what the suite executed**, not a measurement of
what the suite checked. Everything below exists to keep the number from claiming more than
that.

---

## Commands

```
./gradlew koverHtmlReport      # build/reports/kover/html/index.html
./gradlew koverXmlReport       # build/reports/kover/report.xml — the Codecov upload
./gradlew koverLog             # one line, straight to the console
./gradlew koverVerify          # the floors; fails the build when coverage drops through one
```

One merged report for the whole repo rather than a per-module one nobody reads. Kover's
report tasks depend on the `testAndroidHostTest` tasks they measure, so a report task runs
the suites first — there is no "stale coverage" state to get caught by, and running a report
after the test step in CI costs a merge, not a second test run.

Kover is pinned at **0.9.9** in `gradle/libs.versions.toml`. Anything below 0.9.7 does not
understand the KMP Android host-test variant and silently measures nothing.

---

## What is measured

Kover instruments **JVM bytecode produced by the Android host-test run**. That is the whole
mechanism, and every property below follows from it.

| Source set | Counted? | How |
|---|---|---|
| `commonMain` | **yes** | It compiles into the Android host-test classpath, so the Android run measures it |
| `androidMain` | yes | Same run |
| `iosMain` | **no** | Never compiled to JVM bytecode |
| `commonTest` / `androidHostTest` | n/a — test code, not a coverage subject | |

### There is no such thing as an iOS coverage number here

Kover instruments JVM bytecode. Kotlin/Native is **not supported upstream** and cannot be
worked around from this repo. That is a real limit, but it is narrower than it sounds:
`commonMain` is where nearly all shared logic lives, and `commonMain` *is* counted — through
the Android host run. What is invisible is `iosMain`, which is thin platform wiring.

A module in the `iosUnitTest` lane still shows only its host coverage. Running the shared
tests on iOS is a **correctness** signal (see [LAYERS.md](LAYERS.md)), not a coverage one.
Do not read a module's coverage number as evidence about its iOS behaviour.

---

## What is deliberately excluded, and why

Excluding code from coverage is a claim that measuring it would make the number *less*
truthful. Each exclusion below carries that argument. Nothing gets excluded because it is
merely inconvenient to test.

### `:core:testing` — the fakes module

Its production code **is** test infrastructure. The fakes are exercised constantly by every
other module's suite, so including the module reports a very high number for code that ships
to nobody. It inflates the aggregate and tells you nothing about the app.

Enforced in `configureCoverage()`
([`Coverage.kt`](../../gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/Coverage.kt)):
the module returns early and is never registered into the aggregate.

### Codegen

Generated code is never written by hand and never meaningfully "covered" — a generated
`equals`/`hashCode` that no test calls is not a gap anyone should fix.

| Excluded | Generator |
|---|---|
| `*.ComposableSingletons*` | Compose compiler — one per file holding `@Composable` lambdas |
| `*.BuildKonfig` | BuildKonfig |
| `*.generated.resources` packages | Compose Resources (`Res.drawable.*`) |
| `xyz.ksharma.krail.sandook.db` | SQLDelight query and table classes |
| `app.krail.bff.proto`, `app.krail.kgtfs.proto`, `com.google.transit.realtime` | Wire, from the `.proto` definitions |

The SQLDelight filter is a package match only because the generated code was **moved** to make
that possible. It used to be emitted into `xyz.ksharma.krail.sandook`, the same package the
hand-written repositories live in, so no filter could separate the two and all but one
generated file counted as covered production code.

### Declarations, not logic

Three more exclusions are not codegen. They are hand-written code whose *execution* proves
nothing, so counting them measures whether a test happened to touch a file rather than whether
anything is checked:

| Excluded | Why |
|---|---|
| `*ModuleKt`, `*.di.*Kt` | A Koin `module { single { … } }` block is a wiring list. Running it proves the graph parses, which every test that starts Koin already proves. |
| `*.Android*Service` | `expect`/`actual` wrappers around an Android SDK service. A host test cannot drive the real service, so these report as uncovered whatever anyone writes. |
| `*.AnalyticsEvent$*` | Kotlin's generated `componentN`, `copy`, `equals`, `hashCode`, `toString` on a sealed hierarchy of data holders. None is hand-written; none can be wrong. |

### `@Composable`

The largest exclusion, and the one that changes what the number *means*:

```kotlin
annotatedBy("androidx.compose.runtime.Composable")
```

A snapshot test renders every `@PreviewComponent` / `@PreviewScreen`, which walks the composable
tree and marks those lines covered — without asserting a single behaviour beyond "it did not
crash and the pixels match". With composables counted, the number rises when a preview is added
rather than when anything new is checked.

What stays measured is the code where a wrong branch is a bug you can write an assertion about:
reducers, mappers, repositories, ranking, date handling. That is the number worth gating on.

### Where the filters live

All of it lives in `configureCoverage()`, and is applied to **both** each module's own reports
and the root's merged report. That matters: the filters used to sit in the root
`build.gradle.kts` only, so `./gradlew :taj:koverLog` and the taj slice of the aggregate were
computed over different sets of classes and quietly meant different things.

Do not add a `kover { }` block to the root build file. It would apply to the merged report
alone, which is exactly the split that let the two drift.

---

## Which modules are in the aggregate

**Every module with production code** — 58 of them. `configureCoverage()` applies the Kover
plugin and registers a module if it has `.kt` files under `commonMain`, `androidMain` or
`iosMain`. Only `:core:testing` is skipped.

It used to register a module only if that module had *test* sources, which meant 32 modules.
The stated reason was that a module with no tests would report 0%, drowning the signal from
modules that do have suites.

That is a readability argument doing duty as a measurement one. It is true that a wall of 0%
rows makes a report harder to read. It does not follow that the aggregate should exclude them:
the repo-wide percentage was being computed over the friendly half of the repo, and a module
with no tests is precisely the thing an honest number should show. The 26 modules that joined
are visible in the report now, and `verifyTestWiring` is what stops a module quietly having no
suite at all.

---

## Where the reports go

| Destination | What | Retention |
|---|---|---|
| `build/reports/kover/html/index.html` | Local browsable report | until the next build |
| CI build artifact `coverage-report-<run_id>` | The same HTML, per run | 7 days |
| Codecov | `report.xml`, flag `host-tests` | dashboard, PR comments, badge |

Both CI destinations are produced by the `Generate coverage report` step in
[`code-quality.yml`](../../.github/workflows/code-quality.yml), which runs **only if the host
test step succeeded** — partial coverage from a broken run is worse than none.

### The `CODECOV_TOKEN` placement rule

`CODECOV_TOKEN` must live in the **`Firebase` environment**, next to the NSW API keys — not as
a repository secret. `code-quality.yml` is a reusable workflow, and a reusable workflow cannot
see repository secrets.

Both alternatives are worse, and this is why neither was taken:

- `secrets: inherit` on the callers hands `code-quality.yml` **every** repository secret —
  signing keystores, App Store keys, service accounts — to deliver one upload token.
- Declaring `workflow_call.secrets` instead *closes* the secrets context, at which point the
  environment-only `ANDROID_NSW_TRANSPORT_API_KEY` and `IOS_NSW_TRANSPORT_API_KEY` references
  stop resolving and `actionlint` fails the `lint-workflows` job.

The upload is `fail_ci_if_error: false` throughout. A missing token or a Codecov outage must
never fail a pull request; coverage is a signal, not a gate at that layer.

---

## The baseline, and what changed to get it

| | Old measurement | Now |
|---|---:|---:|
| **Merged** | 51.43% over 32 modules | **63.48%** over 58 modules |
| `:feature:trip-planner:ui` | 50.29% | 68.29% |
| `:feature:departures:ui` | 86.87% | 89.12% |
| `:feature:track:ui` | 76.12% | 84.54% |
| `:core:date-time` | 65.00% | 70.27% |
| `:taj` | 50.45% | 49.58% |

Three changes produced that, and they pull in opposite directions:

- Excluding `@Composable` **raises** it, a lot. Most composables in modules without snapshot
  tests were counted and uncovered; removing them removes more uncovered lines than covered
  ones. `:taj` is the exception and the proof — it is nearly all composables *and* has 114
  goldens, so its exclusions were mostly covered lines, and its number went slightly down.
- Registering 26 more modules **lowers** it, by adding whole modules of untested code.
- Deleting 901 lines of unreferenced code raises it slightly, by removing code that was
  uncovered and also unreachable.

Net: up 12 points. **The number went up and the measurement got more honest at the same time,
which is worth being explicit about** — the intuition that a more truthful number must be a
lower one is wrong here. The old number was not flattering because it was too high; it was
untrustworthy because it mixed "a preview rendered" with "a branch was checked", and computed
the total over a hand-picked subset of modules. The new number answers a narrower, answerable
question: **of the non-Composable, non-generated logic in the whole repo, how much does the
host suite execute?**

## Policy: what coverage is allowed to gate

**`koverVerify` floors, and nothing else.**

The floors sit two points under the measured baseline: 61 merged, 87 on `:feature:departures:ui`,
82 on `:feature:track:ui`, 68 on `:core:date-time`. They catch a *regression* — a deleted suite,
a large untested surface landing — and never block ordinary work.

They are not targets. `warningInsteadOfFailure` is deliberately not set: a floor that only warns
is a comment. They are wired into the existing coverage step of `code-quality.yml`, so they cost
a report and no extra test run.

Raising a floor is a deliberate act in its own commit, quoting the new measurement. Lowering one
requires saying what was given up.

Codecov gates nothing. Its project and patch statuses are both `informational`, because a second
gate that can fail a PR on a number computed differently from Kover's produces arguments rather
than tests. Codecov's job is the view: the trend, the per-component breakdown, and the PR comment
saying which new lines went untested.

### Still true, still the main caveat

Coverage measures execution, not assertion. Excluding composables removes the largest source of
that gap, not the gap itself: a `krailRunTest { subject.doThing() }` with no assertion still
covers every line it touches. Nothing in a percentage can see the difference. The
discriminating-test rule in [LAYERS.md](LAYERS.md) and the guards in [GUARDS.md](GUARDS.md) are
what push back on it.

### The golden-coverage companion metric

Line coverage cannot see the difference between a preview that is rendered and a preview whose
rendering is *checked*. The companion metric that can: **what fraction of `@PreviewComponent` /
`@PreviewScreen` previews own a committed golden image.**

That fraction is a direct measure of assertion, not execution — a preview with a golden fails
when its pixels change, a preview without one cannot fail at all. Tracked next to the line
number, it answers the question line coverage silently dodges: of the UI this suite touches,
how much of it is actually pinned?

It is a concept here, not yet a task. The inputs already exist —
`BaseSnapshotTest.packageToScan` enumerates the annotated previews, and the golden PNGs are
committed per module — so computing it is a matter of comparing two lists.

---

## Reading a coverage report without fooling yourself

- **A UI module's number is about its non-UI code.** Composables are excluded, so
  `:feature:trip-planner:ui` at 68% describes its ViewModels, mappers and resolvers, not its
  screens. How well the screens are pinned is a goldens question, not a coverage one.
- **A low number on a module full of `expect`/`actual` platform wiring is expected.** The
  `actual` side that matters on iOS is not measured at all (see above).
- **Coverage never says a test asserts anything.** `krailRunTest { subject.doThing() }` with no
  assertion covers every line it touches. The guards in [GUARDS.md](GUARDS.md) and the
  discriminating-test rule in [LAYERS.md](LAYERS.md) are what push back on that; the
  percentage cannot.
- **Diff coverage is more useful than total coverage.** Codecov's patch status answers "is the
  code this PR added tested", which is the question a reviewer actually has. Total coverage
  answers a question about the past.
