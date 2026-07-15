# KRAIL — Claude Project Notes

## Project

Compose Multiplatform app targeting Android + iOS.
Android is the primary testable target from the command line. iOS tests are not run.

## Analytics events

Firebase caps the app at **500 unique event names, forever**. Before adding or
changing anything in `AnalyticsEvent.kt`, read `docs/ANALYTICS_EVENTS.md` — it has the
new-event-vs-param decision checklist, aggregation patterns (`action`/`source`/boolean
params), the double-counting check, and the EVENT_REGISTRY.md registration gate.
Never mint an event name without passing that checklist.

**Every PR that adds or changes a param/event in `AnalyticsEvent.kt` must also add a
row to `docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same PR.** That file is the living
ledger of what still needs registering in the separate KRAIL-Analytics repo's
`EVENT_REGISTRY.md` — it only stays accurate if it's updated alongside the code change,
not backfilled later.

## Test Commands

| Scope | Command |
|---|---|
| Single module | `./gradlew :feature:track:ui:testAndroidHostTest` |
| Multiple modules | `./gradlew :a:testAndroidHostTest :b:testAndroidHostTest --continue` |
| All modules | `./gradlew testAndroidHostTest --continue` |

**Wrong tasks (do not use):** `jvmTest`, `testDebugUnitTest`, `allTests`

Modules use KMP `androidLibrary { withHostTest {} }` — this creates `testAndroidHostTest`, not the standard AGP task name.

Modules that have `withHostTest {}` enabled:
- `feature/trip-planner/ui`
- `feature/track/ui`
- `feature/track/state`

If a module is missing `testAndroidHostTest`, add `withHostTest {}` inside its `androidLibrary {}` block in `build.gradle.kts`.

## Detekt

```
./gradlew detekt --continue
```

`autoCorrect: true` is set in `detekt.yml` — import ordering and trailing commas are fixed in-place automatically.

Suppression rules:
- Break long lines instead of suppressing `MaximumLineLength` / `MaxLineLength`
- Extract constants instead of suppressing `MagicNumber` (unless truly no reuse value)

### `CyclomaticComplexMethod` — zero tolerance for suppression

**Never** suppress `CyclomaticComplexMethod`. Not via `@Suppress`, not via `detekt.yml`
config, and not via a `baseline.xml` entry (baselining is suppression). When a method
trips the rule, **refactor it** until it passes:
- Extract cohesive blocks into well-named private functions. If the file is also at its
  `TooManyFunctions` limit, extract to a **separate file** so neither limit grows (note:
  detekt counts branches inside nested/local functions toward the enclosing function, so a
  local `fun` does **not** reduce complexity — a top-level function in another file does).
- Replace if/else-if ladders with `when`, polymorphism, or a lookup map.
- Split a Composable that renders many conditional sections into smaller Composables.

If you genuinely believe a method cannot be refactored, stop and raise it with the
maintainer rather than suppressing. There is no "genuinely not possible" escape hatch here.

`LongMethod` is exempt only on `@Composable` functions (already configured); never suppress
it elsewhere — refactor instead.

## LazyColumn / LazyRow item keys

**Always provide an explicit `key` for every `item {}` call** — this is critical for correct
recomposition, scroll-state preservation, and animation behaviour.

```kotlin
// ✅ correct — stable, unique key per item
item(key = "origin-destination") { ... }
item(key = "spacer-top") { ... }
items(journeys, key = { it.journeyId }) { ... }

// ❌ wrong — no key means Compose uses positional identity, which breaks on reorder/insert
item { ... }
```

Key rules:
- Static items use a descriptive string literal (`"spacer-top"`, `"load-more-button"`)
- Dynamic items use a stable domain identifier (e.g. `journeyId`, `stopId`)
- When the same data appears twice in the same list (e.g. previous journeys + main journeys),
  prefix keys to keep them unique: `"prev_$journeyId"` vs plain `journeyId`

## Pull Requests

**Always use Graphite (`gt submit`) to raise PRs — never `gh pr create` directly.**

Exception: the automated docs gardener (single, non-stacked, docs-only PRs labeled
`docs-gardener`, policy in `.github/docs-gardener/CHARTER.md`) may use `gh pr create`.

We stack PRs. Break work into focused, layered branches and submit the full stack with `gt submit --stack --publish`.

**Max 500 lines of change per PR.** If a branch exceeds this, split it before submitting:
- Use `gt branch split --by-commit` or carve out a new child branch
- Each PR should have a single clear concern (ViewModel logic, UI layer, bug fix, etc.)

**Before raising a PR (or when asked to "fix issues and push" / "run quality checks"), run locally and fix all failures first:**

1. Detekt — catches style, formatting, and lint issues before CI does:
   ```
   ./gradlew detekt --continue
   ```
   After detekt runs, always check for auto-corrected files and commit them:
   ```
   git diff --name-only   # stage + commit any files detekt auto-corrected
   ```
   `autoCorrect: true` silently rewrites source files on disk. If those changes
   aren't committed, CI sees the original violations and fails even though local
   detekt reported success.

2. Unit tests — run tests for every module touched by the change:
   ```
   ./gradlew :module:a:testAndroidHostTest :module:b:testAndroidHostTest --continue
   ```

Both must be green before submitting the PR.

## Build

Claude may run build/compile/install commands directly (assembleDebug, installDebug, etc.) — no need to ask the user to run them instead.

## Submodules

KRAIL pulls in the `krail-api-proto` repo as a git submodule at `krail-api-proto/`.
Wire codegen in `:io:bff-api` reads `.proto` files from there. If a fresh checkout
or worktree shows the directory empty, run:

```sh
git submodule update --init --recursive
```

`compileDebugSources` fails with a "no protos found" error otherwise. CI workflows
that compile pass `submodules: true` to `actions/checkout`; if you add a new
workflow that compiles, do the same.

## Worktree build setup

Fresh worktrees are missing gitignored files and build artefacts required to compile
`:androidApp`. Before asking the user to run any build in a worktree, copy all four
of these from the main checkout (`/Users/ksharma/code/apps/KRAIL/`):

```sh
WORKTREE=/Users/ksharma/code/apps/KRAIL/.claude/worktrees/<name>
MAIN=/Users/ksharma/code/apps/KRAIL

# 1. Gradle local config
cp $MAIN/local.properties $WORKTREE/local.properties

# 2. Firebase config (three locations)
cp $MAIN/androidApp/src/debug/google-services.json   $WORKTREE/androidApp/src/debug/google-services.json
cp $MAIN/androidApp/src/release/google-services.json $WORKTREE/androidApp/src/release/google-services.json
cp $MAIN/androidApp/src/main/google-services.json    $WORKTREE/androidApp/src/main/google-services.json
cp $MAIN/composeApp/src/debug/google-services.json   $WORKTREE/composeApp/src/debug/google-services.json
cp $MAIN/composeApp/src/release/google-services.json $WORKTREE/composeApp/src/release/google-services.json

# 3. Wire-generated proto sources (saves a full codegen run)
cp -R $MAIN/io/bff-api/build/generated $WORKTREE/io/bff-api/build/generated

# 4. Proto submodule
git -C $WORKTREE submodule update --init --recursive
```

If any of these are skipped the build fails with one of:
- `File google-services.json is missing` — missing Firebase config
- `Unresolved reference 'app'` in BFF mappers — missing Wire-generated sources or empty submodule
- `Register API key` — missing `local.properties`

## Full Quality Checks

To verify a branch compiles on both platforms and passes static analysis, run:

```
./scripts/fullQualityChecks.sh
```

This runs, in order:
1. `compileDebugSources` — Android compile
2. `compileKotlinIosSimulatorArm64` — iOS Simulator compile
3. `detekt --continue` — static analysis (auto-corrects imports and trailing commas)

Stops on first compile failure. Detekt continues on rule violations so all issues are reported at once.

## Gradle Dependencies

Always use **type-safe project accessors** — never the string form.

```kotlin
// ✅ correct
implementation(projects.composeApp)
implementation(projects.core.log)
implementation(projects.core.deeplink)
implementation(libs.androidx.appcompat)

// ❌ wrong — do not use
implementation(project(":composeApp"))
implementation(project(":core:log"))
```

`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` is active in `settings.gradle.kts`.
The accessor name mirrors the directory path with dots (`core/log` → `projects.core.log`).

## Preview Annotations

Always use the project's custom preview annotations — never bare `@Preview`.

| Annotation | Use for |
|---|---|
| `@PreviewComponent` | Individual components / composables |
| `@PreviewScreen` | Full screens |

Both are defined in `xyz.ksharma.krail.taj.preview`. They expand to multiple device/theme combinations automatically. Using `@Preview` directly produces a single-config preview and misses dark mode, font scale, etc. — this includes when pairing bare `@Preview` with `@ScreenshotTest`; use `@PreviewComponent`/`@PreviewScreen` even for screenshot-tested previews.

Sheets built on `ModalBottomSheet` render via a real `Dialog`/`Popup`, which the IDE's static preview surface can't show. Split the sheet's body into a separate `*Content` composable (no `ModalBottomSheet` wrapper) and preview that directly — the public `*Sheet` function still wraps it in `ModalBottomSheet` for real usage.

## Background polling — WhileSubscribed lifecycle rule

See `docs/POLLING_LIFECYCLE.md` for full rules and patterns.

**TL;DR:** Use `repeatOnLifecycle(STARTED)` inside `LaunchedEffect` to activate
side-effect flows — never plain `LaunchedEffect { launch { flow.collect {} } }`.
Always collect `uiState` with `collectAsStateWithLifecycle()`.

## Per-feature UX rule docs

Some features have a markdown file capturing their UX invariants and outstanding test
coverage gaps. Read these before changing the relevant screen — anything you alter
that contradicts the doc should also update the doc in the same change.

- `feature/trip-planner/ui/SEARCH_STOP_UX.md` — SearchStopScreen (labels, save sheet,
  edit mode, conflict warnings, contextual banner, state persistence).
- `feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md` — address/POI search gate,
  cache, and staleness-token classes in `searchstop/address/`; read before changing
  `onAddressSearchTextChanged` or the `search_stop_address_*` Remote Config contract.
- `docs/TABLET_FOLDABLE_UX.md` — adaptive layout rules for tablets, foldables, and phone
  landscape (per-screen dual-pane behaviour, compact-height adaptations, breakpoint contract).
- `docs/POLLING_LIFECYCLE.md` — WhileSubscribed polling rules: `repeatOnLifecycle(STARTED)`
  pattern, why plain `LaunchedEffect` breaks background gating, all polling flows listed.
- `docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md` — why `TripResponseMapper.kt`'s
  `collapseSameRouteQuickWalks()` merges same-route-number legs split by a trivial walk;
  read before changing leg-merge/split logic in `TripResponseMapper.kt` or
  `TripResponseLegMapper.kt`.
