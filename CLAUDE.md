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
dashboard groupings — there is no contract file to keep in sync and no per-PR analytics
test.

There IS one registration step: **any PR that adds a new event name, or changes params on
an existing event, must add a row to `docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same PR**
(`Status = Pending`). New-event rows get flipped to `Registered` automatically once
KRAIL-Analytics labels them — see `docs/ANALYTICS_REGISTRY_SYNC.md` for how. Param and
user-property rows have no per-item registry surface on the analytics side, so mark those
`Documented` by hand once their shape is final. Read the ledger's own "How to use this
file" section for the exact row format before adding one.

## Test Commands

| Scope | Command |
|---|---|
| Single module | `./gradlew :feature:track:ui:testAndroidHostTest` |
| Multiple modules | `./gradlew :a:testAndroidHostTest :b:testAndroidHostTest --continue` |
| All modules | `./gradlew testAndroidHostTest --continue` |

**Wrong tasks (do not use):** `jvmTest`, `testDebugUnitTest`, `allTests`

Modules use KMP `androidLibrary { withHostTest {} }` — this creates `testAndroidHostTest`, not the standard AGP task name.

**Most modules have `withHostTest {}` now** — roughly half the build files in the repo, and every
module that owns a test suite. Do not keep a list here; it goes stale. Check the module's own
`build.gradle.kts`, or:

```sh
grep -rl "withHostTest" --include=build.gradle.kts .
```

If a module is missing `testAndroidHostTest`, add `withHostTest {}` inside its `androidLibrary {}`
block in `build.gradle.kts`. You will not have to notice this yourself: `verifyTestWiring` fails
the build when a module has test sources but no host-test task, which is exactly the bug that
once hid seven modules' suites.

Full testing doctrine lives in [`TESTING.md`](TESTING.md), with the detail split across
[`docs/testing/LAYERS.md`](docs/testing/LAYERS.md) (what each test layer is for),
[`docs/testing/GUARDS.md`](docs/testing/GUARDS.md) (every custom detekt rule and guard test) and
[`docs/testing/COVERAGE.md`](docs/testing/COVERAGE.md) (what the coverage number does and does not
mean).

## End-to-end flows (Maestro)

`.maestro/` holds two lanes: `smoke/` (three flows) and `nightly/` (lifecycle, rotation and
permission-denial flows too slow to gate a PR on). The nightly runs both on Android and iOS and
**reports only** — a red nightly is a signal for a human, never a trigger for a machine.

**When a lane is red, read [`docs/MAESTRO_TRIAGE.md`](docs/MAESTRO_TRIAGE.md) before
theorising.** It has the artifact map, the order to read it in (`commands.json` first, the
screenshot last), what the run deliberately does not capture (no video, no frames on a passing
step, no device log on iOS), and a table of failure signatures that have each already cost a
wrong diagnosis. Lane conventions and selectors are in [`.maestro/README.md`](.maestro/README.md).

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

## The rider's theme colour has four roles

Before drawing anything in the theme colour, read
[`taj/THEME_COLOUR_ROLES.md`](taj/THEME_COLOUR_ROLES.md).

One accessor per role, because a call site that does not say which role it means cannot be
checked:

| Accessor | For | Adapted |
|---|---|---|
| `themeGroundColor()` | a fill with content drawn on top | no |
| `themeInkColor()` | text, icons, strokes drawn onto a surface | **yes** |
| `themeBackgroundColor()` | the translucent card wash | no |
| `themeDecorColor()` | gradients, ripples, nothing read off it | no |

Pick with one question: is something drawn **on top of** this colour (ground), or is this colour
drawn **on top of** something (ink)? Pass `UI_COMPONENT_CONTRAST_AA` to `themeInkColor()` for
strokes and icons; the 4.5 default is for text. A chip's label is text even though the chip is a
control.

`themeColor()` is deprecated and `ThemeColorRoleRule` flags it. Ink is **derived**, so adding a
theme needs no new colour and no new test.

Two traps, both documented in full in that file: `contrastRatio` ignores alpha, so composite a
translucent ground before measuring against it; and never key a derivation on
`KrailTheme.colors.surface`, which animates for 1500 ms during a theme switch.

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

Exception: the automated analytics registry sync bot (`.github/workflows/analytics-registry-sync.yml`,
single docs-only PRs labeled `analytics-sync`, flipping `docs/ANALYTICS_REGISTRY_HANDOFF.md`
rows from Pending to Registered once KRAIL-Analytics has labelled the event) may use
`gh pr create` **and may auto-merge** — the one bot here allowed to. Every other bot,
including docs-gardener, is explicitly forbidden from auto-merging (docs-gardener's
charter: "Never merge, approve, or enable auto-merge"). This bot is narrower than a
prose-editing bot: its only possible edit is flipping one table cell from `Pending` to
`Registered`, and `scripts/validate_flip_diff.py` independently re-checks the actual
git diff before every push and refuses (no push, no PR, no merge) unless the change is
exactly that. If you ever widen what this bot can touch, remove the auto-merge
exception and put a human back in the loop.

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

## Layout, insets and the keyboard

Before changing a screen's root layout, adding a bottom-anchored input, or touching an
inset modifier, read **`docs/LAYOUT_AND_INSETS.md`**. It holds the one-inset-authority rule,
why `MainActivity` must keep `android:windowSoftInputMode="adjustResize"`, why
`fillMaxSize()` silently does nothing on a non-weighted `Column` child, and the diagnosis
playbook for when a node is drawn somewhere other than where it was laid out.

`scripts/check_layout_invariants.py` enforces the two mechanical parts and runs inside
`fullQualityChecks.sh`.

**When a layout looks wrong on device, measure before theorising.** Log
`onGloballyPositioned { it.positionInRoot().y }` at every level and compare it against a
screenshot taken at the same moment. A node laid out at one position and drawn at another is
the window moving, not the layout, and no amount of re-reading modifiers will show it. Part 2
of the doc has the exact procedure.

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

## Analytics-derived design decisions

When a design decision is justified by KRAIL-Analytics data, add a row to
**`docs/ANALYTICS_ASSUMPTIONS.md`** rather than only writing the finding into a code comment.
Each row records the claim, what depends on it, how to re-check it, what would falsify it, and
when it falls due (three months by default).

`scripts/check_stale_assumptions.py` reports overdue rows and runs as a warning inside
`fullQualityChecks.sh`; `--strict` exits non-zero for a scheduled job.

**The file is qualitative only** — KRAIL is public, so no counts, percentages or rates go in it,
same rule as PR bodies. Write "weekend openings are a substantial share", never the number.

## Learning log

`docs/learning/` records bugs that were expensive to find, and how the finding went wrong.
Add an entry when the first fix was wrong and so was the second, when detekt and tests were
green while the bug was live, or when diagnosis needed instrumentation rather than reading
code. `docs/learning/README.md` has the format and the rules; prefer adding a check over
adding a paragraph.

## Full Quality Checks

To verify a branch compiles on both platforms and passes static analysis, run:

```
./scripts/fullQualityChecks.sh
```

This runs, in order:
0. `check_layout_invariants.py` — manifest and inset-authority guards, then
   `check_stale_assumptions.py` — overdue analytics assumptions (warning only)
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
- `taj/THEME_COLOUR_ROLES.md` — the four theme-colour roles, when to use which, how ink is
  derived, and what each guard holds; read before drawing in the rider's theme colour.
- `docs/LAYOUT_AND_INSETS.md` — inset authority, `adjustResize`, `weight` vs `fillMaxSize`,
  and how to tell a layout bug from the window moving; read before changing a screen root or
  a bottom-anchored input.
- `docs/MAESTRO_TRIAGE.md` — how to diagnose a red Maestro lane: artifact layout, the order to
  read it in, what is not captured, and the known failure signatures.
- `docs/POLLING_LIFECYCLE.md` — WhileSubscribed polling rules: `repeatOnLifecycle(STARTED)`
  pattern, why plain `LaunchedEffect` breaks background gating, all polling flows listed.
- `docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md` — why `TripResponseMapper.kt`'s
  `collapseSameRouteQuickWalks()` merges same-route-number legs split by a trivial walk;
  read before changing leg-merge/split logic in `TripResponseMapper.kt` or
  `TripResponseLegMapper.kt`.
- `docs/SEARCH_QUERY_TELEMETRY_SPEC.md` — what KRAIL may learn about what a rider types into
  search: length and shape always, raw text only under the zero-result carve-out, and the four
  conditions that carve-out requires. Read before touching `SearchQueryAnalytics`,
  `SearchQueryAnalyticsRedaction` or any `search_stop_query` parameter.
- `docs/ANALYTICS_REGISTRY_SYNC.md` — how new-event rows in
  `docs/ANALYTICS_REGISTRY_HANDOFF.md` auto-flip from `Pending` to `Registered`; read
  before touching `.github/workflows/analytics-registry-sync.yml` or its scripts.
- `feature/trip-planner/ui/ASK_KRAIL_UX.md` — the Ask KRAIL surface (`components/ai/`): what the
  three pieces of text on it are for, how the suggestion line is built (situations table,
  fallback ladder, weekend rules and the Sunday exception), the label vocabulary, the speech
  rules, and the layout invariants. Read before changing anything the screen says or shows.
- `feature/trip-planner/ui/AI_SEARCH_UX.md` — the pipeline behind that surface (`search/ai/`):
  the rule that nothing the model produces is ever displayed, and every failure mode with
  whether a test holds it.
- `feature/trip-planner/ui/ALERT_SUMMARY_UX.md` — on-device AI alert summary + vote gating
  (`ALERT_SUMMARY_ENABLED` flag, device availability, call outcome all collapse to "render
  nothing"); read before changing `alerts/summary/` or `CollapsibleAlert.kt`.
