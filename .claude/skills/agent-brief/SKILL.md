---
name: agent-brief
description: The brief every build, QA or review subagent gets, and the standing rules it carries. Use whenever delegating code work to a subagent or a worktree agent, writing a hand-off prompt, or starting a task from someone else's brief. Has the brief template, the build rules that already exist in this repo's docs, what counts as evidence for a claim, and the proof every report must carry.
---

# Agent brief

A brief is a hand-off with no second chance: the agent knows only what is written in it. The
standing rules live here, so a brief points at this file instead of repeating them, and a new
mistake becomes one new line here instead of one more line in every brief after it. Read the
whole file, then write the brief from the template. An agent that receives a brief pointing here
reads the whole file before touching anything.

## 1. The template

Fill every field. An empty field is a decision nobody made.

```
GOAL        One sentence: the outcome, and how it will be checked.
SCOPE       What to change.
NON-GOALS   What to leave alone, even when it looks wrong.
WORKTREE    Path and branch. Work only there. Setup done (docs/WORKTREES.md): yes or no.
READ FIRST  Paths of the docs and real files that decide this task (the ERD, the UX doc, the
            existing component). Not summaries of them.
HARD RULES  Section 2 applies in full. Add only what is specific to this task.
PROOF       The commands that must be green, and each test or guard that must show red then
            green (section 4).
REPORT      Commit list, evidence for each proof, every command with its result, what was not
            done or not verified.
MUST NOT    Push. Open or edit a PR. Touch a phone, emulator or simulator. Enter another
            worktree. Bypass a hook with --no-verify. Add a new component to taj. Widen scope.
```

## 2. Standing build rules

Each rule names where it comes from. If a rule and the code disagree, say so in the report
instead of picking one silently.

1. **Reuse before drawing.** Before drawing any badge, chip, icon, pill, button or row, search
   the whole app for that look (grep by what it is) and reuse the existing component unchanged,
   with only the parameters it exposes. Use taj as it ships: never restyle it (colour, shape,
   size, shadow) and never wrap it in a look-alike. If nothing fits, use the closest component,
   list the gap in the report, and ask before drawing anything new.
   Source: `docs/FEATURE_QUALITY_CHECKLIST.md` sections 1 and 7 and "Working style";
   `docs/features/map-home/ERD.md`, "Taj reuse audit".
2. **Shared fakes in tests.** Use `FakeFlag`, `FakeSandook`, `FakeAnalytics` and the rest from
   `:core:testing` (`core/testing/src/commonMain/kotlin/xyz/ksharma/krail/core/testing/fakes/`),
   added as a test dependency. Never `object : Flag { }` or a private `Fake*` class;
   `verifyNoAdHocBoundaryFakes` fails the build and the pre-push hook for it.
   Source: `docs/testing/GUARDS.md`; `TestWiringVerification.kt`.
3. **No suppression.** Never `@Suppress`, a baseline entry or a config exemption to turn a check
   green. Fix the cause. `CyclomaticComplexMethod` is a hard zero: extract a top-level function,
   in another file if the file is at its function limit.
   Source: `CLAUDE.md` ("zero tolerance for suppression"); `docs/FEATURE_QUALITY_CHECKLIST.md`
   "Working style".
4. **Characters.** No arrow characters, em dashes or middle dots in copy, docs, comments, commit
   text or PR text, and no colon inside a sentence of rider-facing copy.
   Source: `.claude/skills/ui-mockups/SKILL.md` section 5; `.claude/skills/pr-desc/SKILL.md`.
5. **Short width changes the layout, never the items.** When width runs short (narrow phone, 200%
   font, siblings in a row) stack, or wrap whole items with `FlowRow`. A peer item (chip, button,
   badge, a name beside its "to") is never squeezed, shrunk, clipped or truncated to fit a row,
   and text wraps. One exception: a marker or icon that belongs to a text row (dot, pin, label
   icon) stays beside the first line and a long name wraps under it with a hanging indent, never
   a line holding only the marker. Look at 320 dp at 200% font.
   Source: `.claude/skills/ui-mockups/SKILL.md` sections 2 and 4; `docs/FEATURE_QUALITY_CHECKLIST.md`
   section 6 "Text scaling"; `docs/MAP_HOME_UX.md` ("The rows keep the marker on the first line").
6. **One element, one look.** Two states of one element look the same (fill, text, size) unless
   the design says otherwise, and nothing crossfades between two looks to hide a difference.
   Draw the pair side by side across the transition before building it.
   Source: `docs/MAP_HOME_UX.md` ("The card colour follows the app mode, on the map and in the
   list state alike"); `.claude/skills/ui-mockups/SKILL.md` section 3;
   `docs/FEATURE_QUALITY_CHECKLIST.md` section 7.
7. **Every control has an exit.** Prefer a control that is live to one disabled by async state.
   If one must be disabled, or a step must wait, bound the wait and prove the exit with a
   virtual time test: from every reachable state, with the data frozen at Unknown, Loading and
   Failed, some enabled action still finishes the flow.
   Source: `docs/features/intro/ERD.md` units X1-1 to X1-4; `IntroLivenessTest`,
   `IntroFinishNeverStuckTest`.
8. **A message about the world is earned.** "None near", "no results" and "offline" are shown
   only when the data was really read and was not empty (an empty or unanswered source means
   nothing is known, not that nothing exists), or when the claim has its own evidence (a request
   failed and transport was down). Test it with an empty source and with a source that never
   answers.
   Source: `docs/NETWORK_RELIABILITY.md` ("The corollary the UI has to honour");
   `IntroFlowViewModelParkRideLocateTest` (stops that never get ready pass the screen over with
   no message); `docs/FEATURE_QUALITY_CHECKLIST.md` section 4.
9. **Layout probes and insets.** A layout test proves visibility with `assertIsDisplayed()` and
   measures with `getVisibleBoundsInRoot()` (`VisibleBounds.kt` in `:feature:trip-planner:ui`
   androidHostTest). `getUnclippedBoundsInRoot()` in a `*LayoutTest.kt` fails
   `scripts/check_layout_invariants.py`. One inset authority per screen, never
   `safeDrawingPadding()` with `imePadding()` in one file. In panes use `listPaneInsets()` and
   `detailPaneInsets()` (`DualPaneScaffold.kt`). For the side safe area never read `displayCutout`
   (Compose on iOS reports it as 0 at the sides, so a card runs under the foldable iPhone's status
   pill): pad floating cards and controls with `Modifier.sideSafeInsets()` from taj, and
   `scripts/check_layout_invariants.py` fails the build on a raw `displayCutout`. A sheet body pads
   the vertical system bars only, never the sides: `SheetContent` already insets them. Name the
   device profiles a screen must pass (`docs/testing/DEVICE_PROFILES.md`) in the brief.
   Source: `docs/LAYOUT_AND_INSETS.md` 1.6; `docs/testing/GUARDS.md`.
10. **Spec first, cost first.** A feature that touches network, storage, timers or the main
    thread gets its spec and the seven cost answers, with measured numbers, before any code.
    Do not copy the list into the brief.
    Source: `docs/FEATURE_QUALITY_CHECKLIST.md` section 0; `docs/templates/FEATURE_ERD_TEMPLATE.md`.

Run tests with `./gradlew :module:testAndroidHostTest`. The bare `test` task runs nothing here
(`TESTING.md`).

## 3. Claims need evidence

A statement that code does or does not do something (an event fires, a component exists, a value
is absent) is a claim, and a claim carries pasted output.

- **Exists, does not exist, never fires.** Paste the grep. Search the class, event or string name
  over the whole repo (every module, tests, docs, config), never only the screen where you expect
  it. "Absent" from a narrow search is a guess.
- **Works.** Paste the test or command output that showed it. A compile, a detekt run or an
  unrelated test says nothing about behaviour.
- **Data.** Count the rows. Never conclude "absent" from a query with a LIMIT, and state the
  window you looked at. One live reading shows the shape of a response, not a rule.
- **Data another repo or service owns** (analytics, feeds, stores). Ask its owner, or say you
  could not. Never infer it from local caches or fixtures.
- **Not checked.** Say so in words. Never report a result you did not see.

Source: `docs/FEATURE_QUALITY_CHECKLIST.md` section 11; the last paragraph of the QA checklist
in `CLAUDE.md`.

## 4. Proof in every report

- **Red to green for each new test or guard.** Revert the fix, or add the violation, and show it
  fail for the stated reason. Restore it and show it pass. A test that has never failed is
  unproven. Source: `docs/LAYOUT_AND_INSETS.md` section 1.4.
- **Exact commands, with results.** Copy them, with exit codes and counts. Do not paraphrase.
- **Clean tree.** `git status` clean and the commit list at the end.
- **What was not done or not verified.** A list, even when it is empty.
- **Before the first push of a big branch**, run every CI step locally. `./scripts/fullQualityChecks.sh`
  runs the fast CI steps (`scripts/ci_fast_checks.sh`), both compiles and detekt; it does not run
  host tests, the build-logic tests or the coverage floors, so run those as
  `.github/workflows/code-quality.yml` does. Commit any file detekt auto-corrected. Merge current
  `main` in again just before opening the PR and re-run whatever the merge touched.

## 5. This repository is public

Nothing about any person, no usage figures, ratings or revenue, no contents of private
repositories or dashboards, in code, docs, tests, commit text or a report that may be pasted into
a PR. Write findings in words. State a rule in neutral terms, never copy it from private notes.
Full policy: `.claude/skills/pr-desc/SKILL.md`.
