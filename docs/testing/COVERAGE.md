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
| SQLDelight query and table classes | SQLDelight |

> **Known gap as of this document.** The SQLDelight filter is written as
> `packages("xyz.ksharma.krail.sandook.sandook")`, which matches exactly one generated file
> (`KrailSandookImpl`). The other 36 generated query and table classes are emitted into
> `xyz.ksharma.krail.sandook` — **the same package the hand-written repositories live in** —
> so no package filter can separate them and they are all currently counted as covered
> production code. Separating them needs the generated package moved, not a better glob.

### Where the filters live

Today the filter block sits in the **root `build.gradle.kts`**. That means it applies to the
merged report and **not** to a per-module report: `./gradlew :taj:koverLog` and the taj slice
of the merged report do not filter identically. Treat a per-module number as the rougher of
the two.

---

## Which modules are in the aggregate

`configureCoverage()` applies the Kover plugin and registers a module into the root aggregate
**if the module has test sources** (`commonTest`, `androidHostTest` or `androidUnitTest`
containing at least one `.kt` file).

The stated reason for the "has tests" condition: a module with no tests would report 0%, which
is true but drowns the signal from modules that do have suites, and `verifyTestWiring` is what
stops a module quietly having none.

That argument is load-bearing in one direction and misleading in the other. It is right that
a wall of 0% rows makes the report harder to read. It is wrong that the aggregate should
therefore *exclude* them — a module with no tests is precisely the thing an honest number
should show. Excluding it means the headline percentage is computed over the friendly half of
the repo. **See the roadmap below.**

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

## Policy: what coverage is allowed to gate

**Today: raw line coverage, with floors that sit under the current number.**

`koverVerify` enforces a minimum. The floors are set a couple of points below the measured
baseline, so they catch a *regression* — someone deleting a suite, or landing a large
untested surface — and never block ordinary work. They are not aspirational targets, and they
are not `warningInsteadOfFailure`: a floor that only warns is a comment.

Raising a floor is a deliberate act that goes in its own commit with the new measurement
quoted. Lowering one requires saying what was given up.

### Raw line coverage overstates a Compose codebase

This is the honest caveat on every number in this document. In a Compose Multiplatform app a
large share of the lines are `@Composable` declarations, and a **snapshot test executes almost
all of them**. Roborazzi renders every `@PreviewComponent` / `@PreviewScreen`, which walks the
composable tree and marks those lines covered — without asserting a single behaviour beyond
"it did not crash and the pixels match".

So a rise in the number can mean a new preview was added, not that anything new is checked.

### Roadmap: the logic-only gate

The direction is to gate on **logic coverage** and report raw line coverage alongside it as
context.

1. Add `annotatedBy("androidx.compose.runtime.Composable")` to the Kover exclusion filters.
   Kover supports annotation-based filtering directly, so this needs no new tooling. What
   remains measured is the code where a wrong branch is a bug you can write an assertion
   about: reducers, mappers, repositories, ranking, date handling.
2. Move the filters out of the root build file into `configureCoverage()`, so a per-module
   report and the merged report filter identically.
3. Register **every module with production code** into the aggregate, not only the ones that
   already have tests, so the headline is computed over the whole app.
4. Re-measure, and set the floors under the new honest baseline.

Each of those makes the reported number **go down**. That is the point. A number that fell
because the measurement got more honest is worth more than a number that stayed high because
the measurement was flattering.

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

- **A high number on a UI module is mostly previews.** Check whether the covered lines are
  `@Composable` before believing it.
- **A low number on a module full of `expect`/`actual` platform wiring is expected.** The
  `actual` side that matters on iOS is not measured at all (see above).
- **Coverage never says a test asserts anything.** `krailRunTest { subject.doThing() }` with no
  assertion covers every line it touches. The guards in [GUARDS.md](GUARDS.md) and the
  discriminating-test rule in [LAYERS.md](LAYERS.md) are what push back on that; the
  percentage cannot.
- **Diff coverage is more useful than total coverage.** Codecov's patch status answers "is the
  code this PR added tested", which is the question a reviewer actually has. Total coverage
  answers a question about the past.
