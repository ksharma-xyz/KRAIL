# KRAIL — Claude Project Notes

## Project

Compose Multiplatform app targeting Android + iOS.
Android is the primary testable target from the command line. iOS tests are not run.

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
- Only suppress `CyclomaticComplexMethod` / `LongMethod` when refactoring is genuinely not possible

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

We stack PRs. Break work into focused, layered branches and submit the full stack with `gt submit --stack --publish`.

**Max 500 lines of change per PR.** If a branch exceeds this, split it before submitting:
- Use `gt branch split --by-commit` or carve out a new child branch
- Each PR should have a single clear concern (ViewModel logic, UI layer, bug fix, etc.)

## Build

Never run build/compile commands (assembleDebug, etc.) — ask the user to run them and share output.

## Full Quality Checks

To verify a branch compiles on both platforms and passes static analysis, ask the user to run:

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
| `@ScreenPreview` | Full screens |

Both are defined in `xyz.ksharma.krail.taj.preview`. They expand to multiple device/theme combinations automatically. Using `@Preview` directly produces a single-config preview and misses dark mode, font scale, etc.

## Per-feature UX rule docs

Some features have a markdown file capturing their UX invariants and outstanding test
coverage gaps. Read these before changing the relevant screen — anything you alter
that contradicts the doc should also update the doc in the same change.

- `feature/trip-planner/ui/SEARCH_STOP_UX.md` — SearchStopScreen (labels, save sheet,
  edit mode, conflict warnings, contextual banner, state persistence).
