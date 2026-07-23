# KRAIL — Claude Project Notes

## Project

Compose Multiplatform app targeting Android + iOS.
Android is the primary testable target from the command line. iOS tests are not run.

## Analytics events

Firebase caps the app at **500 unique event names, forever**. Before adding or
changing anything in `AnalyticsEvent.kt`, read `docs/ANALYTICS_EVENTS.md` — it has the
new-event-vs-param decision checklist, aggregation patterns (`action`/`source`/boolean
params), and the double-counting check. Never mint an event name without passing that
checklist.

`AnalyticsEvent.kt` is the whole analytics job in this repo. The **KRAIL-Analytics** repo
reads it at the latest published release tag and builds its own registry, labels and
dashboard groupings — there is no contract file to keep in sync, no per-PR analytics test,
and no registration step here. Just define the event well and it reaches analytics after
the next release.

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

**Before writing any PR description or commit message, load the `pr-desc` skill**
(`.claude/skills/pr-desc/SKILL.md`). This repo is public: PR bodies and commit messages
must describe code changes only — no internal metrics, analytics numbers, or
business/strategy context. The skill has the template and full content policy.

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

3. Device QA — see the checklist below. Static checks cannot catch runtime-only bugs.

All three must be green before submitting the PR or handing the branch back.

## QA checklist before handing over code

Detekt and unit tests prove the code compiles and its logic holds. They prove nothing about
what happens on a real device. **Run this before saying a change is ready** — never hand over
a UI change verified only by `./gradlew`.

For any change that touches a screen:

| # | Check | Why it is not covered by detekt/tests |
|---|---|---|
| 1 | `./scripts/fullQualityChecks.sh` green | — |
| 2 | `./gradlew testAndroidHostTest --continue` green | — |
| 3 | Install and open the changed screen | Compilation says nothing about whether it renders |
| 4 | **Rotate the device on every new/changed screen** | Activity recreation crashes are invisible to static checks. See "Configuration changes must never crash" |
| 5 | Rotate again with data loaded AND while loading | Different code paths save different state |
| 6 | Navigate away and back | Catches lifecycle and back-stack restore faults |
| 7 | Check `adb logcat` for `FATAL EXCEPTION` after the run | A crash in a background coroutine may not close the app |
| 8 | Switch theme + light/dark on the screen | Config change AND a contrast check in one pass |
| 9 | Confirm loading, empty and error states each render | Easy to build only the happy path |

Useful commands:

```sh
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # landscape
adb shell settings put system user_rotation 0   # back to portrait
adb logcat -d | grep -A 30 "FATAL EXCEPTION"
```

If a device is not connected, say so plainly and list which of these checks were skipped —
do not describe a change as verified when only the static checks ran.

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

# 5. iOS Firebase config — only needed if you will build the iOS app from this worktree
cp $MAIN/iosApp/iosApp/GoogleService-Info.plist $WORKTREE/iosApp/iosApp/GoogleService-Info.plist
```

If any of these are skipped the build fails with one of:
- `File google-services.json is missing` — missing Firebase config
- `Unresolved reference 'app'` in BFF mappers — missing Wire-generated sources or empty submodule
- `Register API key` — missing `local.properties`
- `Build input file cannot be found: .../GoogleService-Info.plist` — missing iOS Firebase config

Note: the repo no longer uses the `krail-api-proto` submodule, so step 4 is a no-op on current
checkouts. It is kept for branches that predate its removal.

### Running on an iOS simulator

`compileKotlinIosSimulatorArm64` only proves the Kotlin compiles; it does not build or install
the app. To actually run it:

```sh
xcrun simctl list devices booted        # pick the target simulator's UDID

cd $WORKTREE/iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'id=<UDID>' -derivedDataPath <build-dir> build

xcrun simctl install <UDID> "<build-dir>/Build/Products/Debug-iphonesimulator/Krail App.app"
xcrun simctl launch <UDID> xyz.ksharma.krail
```

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

## Building a feature — read the checklist first

Before writing code for any feature that adds a screen, a section, or a new surface for
existing data, read **`docs/FEATURE_QUALITY_CHECKLIST.md`**.

It is a pre-flight list, not a review list: reuse audit, real-data collision check, state
survival across configuration changes, designing all four of loading/empty/error/content up
front, visual weight, font scaling, cross-surface consistency, and shared rate limits. Every
entry is a defect that actually shipped here and was caught by hand, so working through it
first is the difference between one review round and five.

## Configuration changes must never crash — zero tolerance

Rotation, theme switch, font-size change, dark-mode toggle, split-screen and unfolding all
destroy and recreate the Activity. A screen that works until the device rotates is a broken
screen. **Green detekt and green unit tests do not catch this class of bug** — it only
appears at runtime, on a real device, in code paths that compile perfectly.

**Never hand over a screen without exercising a configuration change on it.**

### Navigation routes: register or it will crash

Navigation 3 serialises the entire back stack in `onSaveInstanceState`. A route missing from
the polymorphic `SerializersModule` throws
`SerializationException: Serializer for subclass 'X' is not found in the polymorphic scope of
'NavKey'` — but **only when the Activity is recreated**, never at build time and never on
navigation. The screen works flawlessly until the user rotates, then the app dies.

Every new route MUST be added to
`composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/SerializationConfig.kt`:

```kotlin
subclass(MyNewRoute::class, MyNewRoute.serializer())
```

`NavKeySerializationConfigTest` (in `:composeApp` androidHostTest) walks every sealed route
hierarchy by reflection and fails with the exact missing route names, so this is caught by
`./gradlew :composeApp:testAndroidHostTest` rather than by a user rotating their phone. Do
not delete or weaken that test.

### State that must survive recreation

- Use `rememberSaveable`, not `remember`, for anything the user would notice losing
  (scroll-adjacent flags, expanded/collapsed state, one-shot animation gates, text input).
- Collect `uiState` with `collectAsStateWithLifecycle()`.
- Anything held in a `rememberSaveable` must be `@Serializable`, `Parcelable`, or have an
  explicit `Saver` — an unsupported type also throws only on recreation.

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
