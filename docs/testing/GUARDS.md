# Guards — the checks that hold invariants nobody remembers

Part of [TESTING.md](../../TESTING.md).

An ordinary test checks one class. A **guard** checks the whole repo for a mistake that is
cheap to make, invisible in review, and expensive to find later. Every guard here exists
because the mistake it catches actually shipped.

Two mechanisms:

- **Custom detekt rules** — syntax-tree checks, run by `./gradlew detekt`.
- **Guard tests** — ordinary JVM tests that walk the source tree, run by `testAndroidHostTest`.

Plus two Gradle-level and one Python-level check that fit neither box.

---

## The ratchet contract

Almost every guard has an escape hatch: a baseline or allowlist of pre-existing offenders. The
contract on all of them is the same, and it is the reason they are worth having:

1. **The list only ever shrinks.** Fixing an offender means deleting its line in the same
   change. Adding a line to make a *new* violation pass is not a use of the file.
2. **Every entry carries its reason**, in a comment on the line or beside it.
3. **When the list reaches zero, delete the file.** The guard then becomes an unconditional
   gate, which is the destination.
4. **A stale entry is a failure.** The better guards fail when an allowlist excuses something
   that no longer exists — otherwise the list rots into a list of things that were once true.

Point 4 is the one that separates a live ratchet from a graveyard, so the tables below say for
each guard whether it enforces it.

One mechanical detail that makes the whole thing work: `Detekt.kt` registers
`config/*-baseline.txt` as a **task input** on every detekt task. Without that, deleting a
baseline entry would leave every detekt task `UP-TO-DATE` and the newly un-grandfathered
violation would never be reported.

---

## Custom detekt rules

Ruleset id `krail`, in the `gradle/build-logic` included build:
[`gradle/build-logic/detekt-rules/`](../../gradle/build-logic/detekt-rules/). Registered in
`KrailRuleSetProvider.kt`, wired onto every module by `configureDetekt()` in
[`Detekt.kt`](../../gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/Detekt.kt)
as `detektPlugins "xyz.ksharma.krail.gradle:detekt-rules:unspecified"` — a coordinate, not a
project reference, because composite-build substitution resolves it.

All eight run in `./gradlew detekt`. Config lives in the `krail:` block of
[`config/detekt.yml`](../../config/detekt.yml).

### `ClockSystemBan`

Flags `Clock.System` read inline, including the fully-qualified form and the
`import kotlin.time.Clock.System` alias evasion. Inline time reads are untestable: nothing can
move them.

**Instead:** read time from the injected `Clock` — a constructor parameter, or a `now:`
parameter.

Exempt: parameter default values (walking *up*, so `Clock.System.now().plus(1.minutes)` as a
default is exempt whole), Koin binding files (a `di` package segment plus an `org.koin`
import), and every test source set — matched by **source-set directory, never by file name**.

| | |
|---|---|
| Baseline | [`config/clock-system-baseline.txt`](../../config/clock-system-baseline.txt) — 14 rows, `path\|count` |
| Ratchet | The count is an **allowance**: offenders beyond it report, so a listed file cannot grow a new read. Shrink-only by header rule, not machine-enforced. |
| Note | Counts, not line numbers — line numbers churn on every unrelated edit, and a stale baseline is worse than none. Cost: in a baselined file the reported location is the *last* read in the file, not the new one. |

### `CollectAsStateBan`

Flags `collectAsState(` calls and its import. `collectAsState()` keeps collecting while the
screen is backgrounded, so polling never pauses and a `WhileSubscribed` upstream never sees
its subscriber count drop.

**Instead:** `collectAsStateWithLifecycle()`. See [`docs/POLLING_LIFECYCLE.md`](../POLLING_LIFECYCLE.md).

No baseline — zero offenders, unconditional gate. Matched exactly, so
`collectAsStateWithLifecycle` does not trip it.

### `CyclomaticSuppressBan`

Two routes, both banned:

1. `@Suppress("CyclomaticComplexMethod")` on a declaration or as `@file:Suppress`.
2. A `CyclomaticComplexMethod:` entry in a module's `baseline.xml` — **baselining is
   suppression**, and this route has no escape hatch at all.

**Instead:** refactor. Extract cohesive blocks to *top-level private functions* — a local `fun`
does not reduce the count, because detekt attributes its branches to the enclosing function.
Or replace the branch ladder with a `when` or a lookup map.

> `config/cyclomatic-suppress-baseline.txt` **does not exist**. `RatchetBaseline` finds the
> repo root by looking for that file, so with it absent nothing is grandfathered and the rule
> is a hard zero-tolerance gate today — stricter than its own KDoc and the `detekt.yml`
> comment describe. That is the intended destination, reached early. Do not create the file to
> get out of a refactor.

### `LazyItemKeyRule`

Flags an `item { }` call with an empty argument list inside a lazy container (`LazyColumn`,
`LazyRow`, the four grid variants) or a `LazyListScope` / `LazyGridScope` /
`LazyStaggeredGridScope` extension function. Without a key, Compose identifies the item by
position, so inserting or reordering hands one row's remembered state — scroll, expanded flags,
running animations — to a different row.

**Instead:** a descriptive string literal for a static item, a domain id for a dynamic one. See
CLAUDE.md, "LazyColumn / LazyRow item keys".

`items(...)` is **deliberately out of scope**: its keyed and unkeyed forms are told apart by
named arguments across several lines and it has count/list/array overloads, so a syntactic
check false-positives. Extending it means matching named arguments on the resolved call.

| | |
|---|---|
| Baseline | [`config/lazy-item-key-baseline.txt`](../../config/lazy-item-key-baseline.txt) — 2 rows, `path\|function\|count` |
| Ratchet | Shrink-only by header rule: decrement the count in the same change, delete the line at zero. |

### `PublicImplementationClass`

Flags a public top-level class that implements an interface (`: Foo` or `: Foo by delegate`,
never `: Foo(args)` — that is always a superclass constructor call like `ViewModel()`).

**Instead:** make it `internal`, and depend on the interface everywhere else. Or add the
supertype to `excludeSuperTypes` in `detekt.yml` if it genuinely must stay public.

24 supertypes are excluded by default (`Parcelable`, `Comparable`, `CoroutineScope`, `NavKey`,
`KoinComponent`, the Compose `*ModifierNode` family, …). `:core:testing` is excluded wholesale —
its `Fake*` classes are deliberately public shared test doubles.

Notably it does **not** use type resolution. The `@RequiresTypeResolution` version was written
and verified to produce **zero** findings against a deliberately broken sample on both
`detektAndroidMain` and `detektMetadataCommonMain`, while the same logic worked in a unit-test
harness with resolution set up by hand. Hence raw supertype-name matching.

### `ScreenshotPreviewAnnotation`

Flags a `@ScreenshotTest` function annotated with neither `@PreviewComponent` nor
`@PreviewScreen`.

A bare `@Preview` records **one** light-mode phone baseline. The project annotations expand to
light, dark and 2× font scale, plus tablet for `@PreviewScreen`. So with a bare `@Preview`, a
dark-mode contrast regression or a 2×-font clipping bug lands green.

| | |
|---|---|
| Baseline | [`config/screenshot-annotation-baseline.txt`](../../config/screenshot-annotation-baseline.txt) — 33 rows, `path\|function` |
| Ratchet | Shrink-only by header rule. Fixing one means swapping the annotation, **re-recording the goldens**, and deleting the line in the same change. |

### `ThemeColorRoleRule`

Two things, both about the rider's theme colour:

1. `themeColor()`, the deprecated accessor. It could not say which of the colour's four roles it
   meant, so nothing could check any of them.
2. `themeGroundColor()` — the deliberately unadapted fill colour — passed to a `color =`, `tint =`,
   `textColor =` or `contentColor =` argument, or into `BorderStroke(`. Those positions draw ink
   onto a surface, and some theme colours do not clear WCAG AA there: Purple Drip measured 2.95:1
   on the dark surface and 2.49:1 on a bottom sheet.

**Instead:** `themeInkColor()`, or `themeInkColorOn(background)` when the ground is not one of the
app's own surfaces. `themeDecorColor()` is the sanctioned way out for a gradient stop or ripple
that nothing is read off — saying so explicitly is what lets this rule stay an unconditional gate
instead of accumulating a baseline.

`color =` means a fill on some callees and a stroke on others: `Modifier.background()` takes a
ground, `Modifier.border()` takes ink, and both name the argument `color`. So the callee is part of
the decision and fill-taking callees are skipped. Without that the rule flags a chip's selected
background, where the ground accessor is right and the call site has no correct fix available.

No type resolution, so the check is syntactic. Two limits: a ground colour laundered through a
local `val` first is invisible to it, and a fill-taking callee outside the listed set would
false-positive. `ThemeInkContrastTest` in `:taj` covers the values themselves; this covers the
shape of the call.

No baseline — zero offenders, unconditional gate.

### `SnackbarBan`

Flags any callee or import whose name contains `snackbar`, case-insensitively — so `Snackbar`,
`SnackbarHost`, `SnackbarHostState` and `showSnackbar` all trip it.

A snackbar puts undo on a timer, somewhere other than the thing being undone, on top of the
bottom-anchored controls.

**Instead:** long-press to enter edit mode with an in-place action, or a `ModalBottomSheet` when
a decision needs confirming.

No baseline — zero offenders, unconditional gate. It exists because
`Scaffold { snackbarHost = … }` is the default answer in every Compose sample, so it arrives by
copy-paste rather than by decision.

### Running the rules' own tests

```
./gradlew -p gradle/build-logic :detekt-rules:test
```

**`./gradlew detekt` does not run these.** `detektPlugins` resolves the rules' *jar*, and Gradle
builds only the producing task chain (`compileKotlin`, then `jar`); `test` is never in that graph,
and a root-build task name cannot address tasks in an included build. So a rule could quietly
stop flagging anything with CI still green — which is why `code-quality.yml` has a separate
`Run build-logic tests` step. `fullQualityChecks.sh` does not run them either.

Each rule has a unit test asserting both what it flags and what it must not, and
`RatchetBaselineTest` covers the shared baseline mechanism against a real temp-dir fake repo.

---

## Guard tests

Ordinary tests that walk the source tree. They find the repo root by walking up to
`settings.gradle.kts`, and skip `build/` and `.git/`.

### In `:composeApp` — `./gradlew :composeApp:testAndroidHostTest`

| Guard | Enforces | Allowlist | Stale check |
|---|---|---|---|
| `PollingLifecycleGuardTest` | (a) every `SharingStarted.WhileSubscribed(` in production source is registered as `` `Owner.flowName` `` in [`docs/POLLING_LIFECYCLE.md`](../POLLING_LIFECYCLE.md); (b) no `collectAsState(` anywhere | the doc itself is the register | **no** — a doc row for a deleted flow can rot |
| `DualPaneCompositingGuardTest` | no `DualPaneScaffold(` nested inside a compositing wrapper (`CloudGradientBackground(`, `.graphicsLayer`, `.blur(`) | in-file: `.blur(` in `SavedTripsScreen.kt` | no |
| `LearningLogActionsTest` | every backticked identifier inside a ticked `- [x]` action in `docs/learning/*.md` resolves — a path exists on disk, a `*Test` name is declared in a test source set and the named method is in that file | none | n/a |
| `ViewModelStateDurabilityTest` | every ViewModel registered in a Koin module takes a `SavedStateHandle` in some constructor, or is listed with a reason | [`config/no-saved-state-allowlist.txt`](../../config/no-saved-state-allowlist.txt) — 19 entries | **yes** |
| `RoutePaneMetadataTest` | the pane-metadata table in [`docs/TABLET_FOLDABLE_UX.md`](../TABLET_FOLDABLE_UX.md) exactly equals the `entry<Route>(metadata = …)` declarations in source | the doc table is the register | **yes**, both directions |
| `NavKeySerializationConfigTest` | every concrete subclass of every sealed route hierarchy is registered in the polymorphic `SerializersModule` | none | no |

`DualPaneCompositingGuardTest` and `NavKeySerializationConfigTest` both hold crash classes that
**only appear at runtime**: an iOS map that renders blank under a gradient, and a
`SerializationException` thrown the first time a user rotates. Neither is visible to the
compiler.

`ViewModelStateDurabilityTest` also fails hard if it finds **zero** ViewModel registrations —
a scan-broke detector, so a refactor that defeats the regex fails loudly instead of passing
vacuously. Several guards here carry that pattern; copy it into any new one.

### Elsewhere

| Guard | Module | Enforces | Allowlist | Stale check |
|---|---|---|---|---|
| `TurbineHygieneTest` | `:core:testing` | a `.test { }` block whose subject names itself a poller/ticker/auto-refresh or an `isActive` gate must cancel: `cancelAndIgnoreRemainingEvents()`, `cancelAndConsumeRemainingEvents()` or a bare `cancel()` | [`config/turbine-hygiene-allowlist.txt`](../../config/turbine-hygiene-allowlist.txt) — **0 entries**, empty on purpose | **yes** |
| `SearchQueryTextEgressTest` | `:feature:trip-planner:ui` | drives the real analytics helpers and asserts no property of the built `search_stop_query` event contains the query or any ≥4-char token of it; a control test asserts the zero-result carve-out still does emit it | none | n/a |
| `SearchQueryRedactionCallSiteTest` | `:feature:trip-planner:ui` | every `zeroResultQuery =` assignment has a redaction call within 8 lines | none | n/a |
| `BrandHexUniquenessTest` | `:feature:trip-planner:ui` | no production `.kt` outside `core/transport/` contains a `TransportMode.*.colorCode` hex literal | in-file, 13 entries | **yes** |
| `KrailColorsParityTest` | `:taj` | (a) no `KrailColors` constructor parameter has a default — a default lets a scheme silently omit a token; (b) every token differs between light and dark unless declared invariant | in-file `INTENTIONALLY_MODE_INVARIANT`, 6 entries | **yes** |
| `KrailColorTokenContrastTest` | `:taj` | 14 foreground/background pairings clear WCAG AA in both schemes, foreground composited over background first; **and** every `KrailColors` property is classified as either a pairing or explicitly not-a-contrast-pair | in-file: 6 known failures, 13 not-a-pair | **yes**, both directions |
| `ThemeContrastTest` | `:taj` | every `KrailThemeStyle` yields an AA-passing foreground, and overrides a low-contrast foreground handed to it rather than honouring it | none | self-healing over the enum |
| `TransportModeContrastTest` | `:feature:trip-planner:ui` | every transport-mode colour clears AA for UI components against both surfaces | in-file, 5 entries with measured ratios | **yes** |
| `NswTransportLineContrastTest` | `:feature:trip-planner:ui` | every NSW line colour passes on at least one surface and is never failing on both; plus a **surface-token drift guard** asserting the light surface stays near-white and the dark one near-black | in-file `officialNswExceptions`, 5 entries | no — and the set is duplicated across two test methods |

The contrast guards share their thresholds from
[`ContrastAnalyzer.kt`](../../taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/contrast/ContrastAnalyzer.kt)
(`TEXT_CONTRAST_AA` 4.5, `UI_COMPONENT_CONTRAST_AA` 3.0). Several are **self-healing**: they
enumerate an enum or parse the token list out of `toString()`, so a new token cannot be added
without being classified. That is the pattern to copy — a guard that iterates a hand-written
list stops covering anything the moment someone forgets to extend the list.

---

## Gradle verification tasks

Registered by the convention plugins, `dependsOn`'d by every module's `check`, and run in CI by
the **Verify test wiring** step of `code-quality.yml`:

```
./gradlew verifyTestWiring verifyTestingModuleUsage verifyNoAdHocBoundaryFakes \
          verifyIosTestClassification --continue -PciQuality=true
```

They run **before** detekt and the tests on purpose, so a misconfigured module fails fast with
a clear message instead of silently skipping its suite.

| Task | Fails when | Baseline |
|---|---|---|
| `verifyTestWiring` | a module has `.kt` under `commonTest` / `androidHostTest` / `androidUnitTest` but no `testAndroidHostTest` task — someone forgot `withHostTest {}`, so CI was silently skipping the suite | none |
| `verifyTestingModuleUsage` | any configuration whose name does not contain `test` declares a dependency on `:core:testing` — stops fakes shipping in the app and breaks the `:core:testing` / feature / `:core:testing` dependency cycle | none |
| `verifyNoAdHocBoundaryFakes` | a new `object : Boundary { … }` stub or `private class Fake<Boundary>` appears in a test source set | [`config/test-wiring-baseline.txt`](../../config/test-wiring-baseline.txt) |
| `verifyIosTestClassification` | a module has shared test sources but is in neither the include nor the exclude list in [`IosUnitTests.kt`](../../gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/IosUnitTests.kt) | the two Kotlin lists themselves |

`verifyNoAdHocBoundaryFakes` carries the **strongest ratchet in the repo**: a hard count cap.
`MAX_BASELINE_ENTRIES = 8` in `TestWiringVerification.kt`, and the baseline currently holds
**exactly 8 entries**. Adding a ninth line fails the build. A new ad-hoc boundary fake is not
grandfathered by adding a line here — the number only ever moves down.

`verifyIosTestClassification` is bidirectional: a module listed in either list that no longer
has shared test sources fails. It also runs on the cheap Ubuntu runner, so classification drift
is caught on every PR rather than only on the macOS lane.

---

## `scripts/check_layout_invariants.py`

Runs as step 0 of `fullQualityChecks.sh` and as the **Layout invariants and analytics
assumptions** CI step. Exits non-zero on a violation. Enforces the mechanical parts of
[`docs/LAYOUT_AND_INSETS.md`](../LAYOUT_AND_INSETS.md):

1. `MainActivity` keeps `android:windowSoftInputMode="adjustResize"` in the manifest — without
   it the system pans the window on top of Compose's IME insets, drawing an input above the
   keyboard and clipping content off the top.
2. No file applies two inset authorities at once (`safeDrawingPadding()` **and** `imePadding()`).
   In-file allowlist: `SavedTripsScreen.kt`, `AskKrailScreen.kt`.
3. No `*LayoutTest.kt` uses `getUnclippedBoundsInRoot()` — it reports where a node was laid out,
   not where it was drawn, which is exactly the distinction those tests exist to make. Kotlin
   comments are stripped first, so a KDoc explaining the ban is fine.

The sibling `check_stale_assumptions.py` in the same CI step is **warning-only**: an analytics
assumption falling due is not a reason to block an unrelated PR. A scheduled job runs it
`--strict`.

---

## Where each `config/` file is consumed

| File | Entries | Consumer |
|---|---:|---|
| `clock-system-baseline.txt` | 14 | `ClockSystemBan` (detekt) |
| `lazy-item-key-baseline.txt` | 2 | `LazyItemKeyRule` (detekt) |
| `screenshot-annotation-baseline.txt` | 33 | `ScreenshotPreviewAnnotation` (detekt) |
| `test-wiring-baseline.txt` | 8 — **at its cap** | `verifyNoAdHocBoundaryFakes` (Gradle) |
| `no-saved-state-allowlist.txt` | 19 | `ViewModelStateDurabilityTest` (`:composeApp`) |
| `turbine-hygiene-allowlist.txt` | 0 | `TurbineHygieneTest` (`:core:testing`) |
| `detekt.yml` | — | every detekt task |
| `baseline.xml` | 22 IDs | **nothing** — see below |

Two loose ends worth knowing about before you trust a file in here:

- **`config/baseline.xml` is dead.** `Detekt.kt` points each module's baseline at its own
  `projectDir`, and the root project applies detekt with `apply false`, so no task reads it.
  Its contents duplicate `composeApp/baseline.xml`. [`docs/lint.md`](../lint.md) still describes
  it as the global baseline; that claim is stale.
- **`config/cyclomatic-suppress-baseline.txt` is absent** while `CyclomaticSuppressBan` and the
  `detekt.yml` comment both name it. The effect is stricter, not looser (see above).

---

## Adding a guard

Before writing one, check it earns its place: a guard is justified by a mistake that is
**invisible in review** and **not caught by an ordinary test**. If a normal unit test would
catch it, write the unit test.

Then:

1. **Prefer a detekt rule to a guard test** when the check is syntactic. It reports at the
   exact offending line, runs on every module, and has somewhere obvious for its own unit test
   to live.
2. **Make it self-healing.** Enumerate the enum, reflect over the class, parse the register —
   never iterate a hand-written list that someone must remember to extend.
3. **Add a scan-broke detector.** Assert the scan found a non-zero number of candidates, so a
   refactor that defeats your matching fails loudly instead of passing vacuously.
4. **If it needs an allowlist**, give it the ratchet contract above — including the stale-entry
   check, which is the part people skip.
5. **Put the reason in the failure message**, not only in a doc. The person who trips it is
   mid-task and will read exactly one thing.
