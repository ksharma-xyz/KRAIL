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

## Build

Never run build/compile commands (assembleDebug, etc.) — ask the user to run them and share output.
