# The quality gate that already existed, and the CI run that paid for skipping it

**2026-09-22** · **Process / a check that was never run** · **Cost:** one red CI cycle, one push

## Symptom

`ios-unit-tests` failed on a PR that added map attribution. Not a test failure:

```
> Task :verifyIosTestClassification FAILED
Execution failed for task ':verifyIosTestClassification'.
```

Nothing about the change touched iOS, tests or CI configuration. The PR enabled an ornament
flag, moved an alignment constant and added three assertions.

## Root cause

The change added the first test source set to `:core:maps:ui`. `IosUnitTests.kt` verifies that
every module with shared test sources appears in either `IOS_TEST_MODULES` or
`IOS_TEST_EXCLUSIONS`, so a module cannot quietly opt out of the iOS lane. A new `commonTest`
directory with no classification is exactly what it exists to catch.

The guard worked perfectly. It ran in the wrong place: CI, minutes later, instead of locally,
in seconds.

## Why it took so long

It did not take long to diagnose. What is worth recording is why it reached CI at all, because
the answer is not "a missing check".

Before pushing, these were run and were all green:

- `./gradlew compileDebugSources`
- `./gradlew detekt --continue`
- `./gradlew :core:maps:ui:testAndroidHostTest`
- `./gradlew :feature:trip-planner:ui:testAndroidHostTest :composeApp:testAndroidHostTest`

That looks like thorough local verification, and it is the trap. Every one of those commands
was chosen to match the change: a compile, a lint, the tests in the modules touched. None of
them is a **structural** check, and the defect was structural.

`scripts/fullQualityChecks.sh` runs `verifyIosTestClassification` alongside `verifyTestWiring`,
`verifyTestingModuleUsage` and `verifyNoAdHocBoundaryFakes`, **before** the compiles, with a
comment explaining that they are seconds-long structural checks placed first on purpose. It
would have failed in under fifteen seconds.

So there was no gap in the tooling. The gap was running four targeted Gradle commands that
each answered "is my change correct?" rather than the one script that answers "is the repo
still coherent?". Those are different questions, and passing the first says nothing about the
second.

The wrong instinct, named so it is recognisable next time: **a small, obviously-scoped change
invites a small, obviously-scoped check.** Adding a test file feels like it cannot break
anything structural, which is precisely when a structural guard is the only thing watching.

## What would have caught it sooner

Running the script the repo already provides.

```sh
./scripts/fullQualityChecks.sh
```

`CLAUDE.md`'s QA checklist has it as item 1, and the "before raising a PR" section lists it
first. Both were followed in spirit and skipped in fact, by substituting the individual tasks
the script wraps.

A useful test for whether the substitution is safe: **the targeted commands are a subset of the
script, so they can only ever find a subset of the problems.** If the reason for not running
the script is speed, note that the structural checks run first for exactly that reason and
fail before anything expensive starts.

## The complication, recorded because it is the reason to skip

On a machine with Xcode 27 the script **cannot finish**. Step 0 (structural checks) and step 1
(Android compile) pass, then step 2 fails in the Swift package build, because no Kotlin version
supports Xcode 27 yet and the plugin's default deployment target is below the floor Xcode 27
accepts. Nothing to do with the change under test.

That is a genuine incentive to reach for targeted commands instead, and it is how a skipped gate
stops looking like a shortcut and starts looking like the only option.

The part that makes it a false economy: **the structural checks run first and print their own
heading.** A run that reaches `▶ Android compile...` has already cleared them. So even on a
machine where the script cannot complete, running it until it fails at the iOS step gives the
full structural verdict, which is exactly what was missed here.

## Actions taken

- [x] `:core:maps:ui` classified into `IOS_TEST_MODULES`, in the lane rather than the
      exclusions, because nothing is known to block it and every exclusion entry records a
      specific verified wall. If the MapLibre cinterop turns out to block linking the
      Kotlin/Native test binary the way the Firebase frameworks do, CI will say so and it moves
      across with the real error as its reason.
- [x] This entry, as the record of a check that existed and was not run.
- [ ] Optional, not done: a `pre-push` hook running only the four structural tasks. They are
      seconds long and order-independent, so the cost is small. Not added unilaterally, since a
      hook changes the workflow of everyone who clones the repo and that is a decision rather
      than a fix.

## The transferable part

A guard that fires in CI rather than locally is not a guard that failed. It is a guard whose
local invocation was skipped, and the fix is upstream of the code.

Worth asking before any push: *did I run the repo's gate, or four commands shaped like my own
change?*
